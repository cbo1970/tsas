package com.cas.tsas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the Clean Architecture / modular-monolith boundaries described in the SAD so they
 * cannot silently erode: a framework-free domain, inward-pointing layer dependencies and an
 * acyclic module graph.
 *
 * <p>The rules are checked against the Java <em>source</em> (package and import declarations)
 * rather than bytecode. A bytecode-based tool (ArchUnit) was evaluated but its bundled ASM
 * cannot yet parse Java&nbsp;25 class files, so the source-based approach is both portable
 * across JDKs and directly readable. The module root is provided via the {@code tsas.rootDir}
 * system property set by the Gradle {@code :app:test} task.
 */
class ArchitectureTest {

    /** Maps a Gradle module directory to the short name used in its base package. */
    private static final Map<String, String> MODULES = Map.of(
            "common-module", "common",
            "player-module", "player",
            "match-module", "match",
            "statistics-module", "statistics",
            "ai-module", "ai",
            "auth-module", "auth",
            "app", "app");

    private static final List<JavaFile> FILES = loadFiles();

    private record JavaFile(String moduleShortName, String pkg, List<String> imports, Path path) {
        boolean inLayer(String layer) {
            return pkg.contains("." + layer + ".") || pkg.endsWith("." + layer);
        }
    }

    @Test
    void domain_is_free_of_framework_dependencies() {
        List<String> violations = new ArrayList<>();
        for (JavaFile f : FILES) {
            if (!f.inLayer("domain")) {
                continue;
            }
            for (String imp : f.imports) {
                if (imp.startsWith("org.springframework.")
                        || imp.startsWith("jakarta.persistence.")
                        || imp.startsWith("com.fasterxml.jackson.")) {
                    violations.add(f.path + " imports " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "domain must stay framework-free (no Spring, JPA or Jackson):\n"
                        + String.join("\n", violations));
    }

    @Test
    void domain_does_not_depend_on_application_or_infrastructure() {
        List<String> violations = new ArrayList<>();
        for (JavaFile f : FILES) {
            if (!f.inLayer("domain")) {
                continue;
            }
            for (String imp : f.imports) {
                if (imp.startsWith("com.cas.tsas.")
                        && (imp.contains(".application.") || imp.contains(".infrastructure."))) {
                    violations.add(f.path + " imports " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "dependencies must point inward: domain must not know application or infrastructure:\n"
                        + String.join("\n", violations));
    }

    @Test
    void application_does_not_depend_on_infrastructure() {
        List<String> violations = new ArrayList<>();
        for (JavaFile f : FILES) {
            if (!f.inLayer("application")) {
                continue;
            }
            for (String imp : f.imports) {
                if (imp.startsWith("com.cas.tsas.") && imp.contains(".infrastructure.")) {
                    violations.add(f.path + " imports " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "the application layer must not depend on infrastructure (ports & adapters):\n"
                        + String.join("\n", violations));
    }

    @Test
    void modules_form_an_acyclic_graph() {
        Set<String> shortNames = new LinkedHashSet<>(MODULES.values());
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        shortNames.forEach(m -> edges.put(m, new LinkedHashSet<>()));

        for (JavaFile f : FILES) {
            for (String imp : f.imports) {
                if (!imp.startsWith("com.cas.tsas.")) {
                    continue;
                }
                String[] parts = imp.split("\\.");
                if (parts.length < 4) {
                    continue; // e.g. com.cas.tsas.TsasBackendApplication — no functional module segment
                }
                String target = parts[3];
                if (shortNames.contains(target) && !target.equals(f.moduleShortName())) {
                    edges.get(f.moduleShortName()).add(target);
                }
            }
        }

        List<String> cycle = findCycle(edges);
        assertTrue(cycle.isEmpty(),
                () -> "module dependency graph must be acyclic, but found cycle: "
                        + String.join(" -> ", cycle));
    }

    // --- helpers -----------------------------------------------------------

    private static List<JavaFile> loadFiles() {
        String root = System.getProperty("tsas.rootDir");
        if (root == null) {
            throw new IllegalStateException(
                    "System property 'tsas.rootDir' is not set (configured by the Gradle :app:test task)");
        }
        Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
        Pattern importPattern = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;");

        List<JavaFile> files = new ArrayList<>();
        MODULES.forEach((moduleDir, shortName) -> {
            Path src = Path.of(root, moduleDir, "src", "main", "java");
            if (!Files.isDirectory(src)) {
                return;
            }
            try (Stream<Path> paths = Files.walk(src)) {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    String pkg = null;
                    List<String> imports = new ArrayList<>();
                    try {
                        for (String line : Files.readAllLines(p)) {
                            Matcher pm = packagePattern.matcher(line);
                            if (pkg == null && pm.find()) {
                                pkg = pm.group(1);
                                continue;
                            }
                            Matcher im = importPattern.matcher(line);
                            if (im.find()) {
                                imports.add(im.group(1));
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    if (pkg != null) {
                        files.add(new JavaFile(shortName, pkg, imports, p));
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        if (files.isEmpty()) {
            throw new IllegalStateException("No source files found under " + root);
        }
        return files;
    }

    /** Returns the nodes of the first dependency cycle found, or an empty list if acyclic. */
    private static List<String> findCycle(Map<String, Set<String>> edges) {
        Set<String> visited = new LinkedHashSet<>();
        for (String node : edges.keySet()) {
            List<String> stack = new ArrayList<>();
            List<String> cycle = dfs(node, edges, visited, new LinkedHashSet<>(), stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of();
    }

    private static List<String> dfs(String node, Map<String, Set<String>> edges,
                                    Set<String> visited, Set<String> onPath, List<String> stack) {
        if (onPath.contains(node)) {
            List<String> cycle = new ArrayList<>(stack.subList(stack.indexOf(node), stack.size()));
            cycle.add(node);
            return cycle;
        }
        if (!visited.add(node)) {
            return List.of();
        }
        onPath.add(node);
        stack.add(node);
        for (String next : edges.getOrDefault(node, Set.of())) {
            List<String> cycle = dfs(next, edges, visited, onPath, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        onPath.remove(node);
        stack.remove(stack.size() - 1);
        return List.of();
    }
}
