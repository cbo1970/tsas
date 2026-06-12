/**
 * Shared Kernel of the TSaS backend (common-module).
 *
 * <p>Provides cross-cutting building blocks used by all functional modules: shared
 * exceptions (e.g. {@link com.cas.tsas.common.exception.ConflictException}), the central
 * web error handling ({@link com.cas.tsas.common.web.CommonExceptionHandler}) and CORS
 * configuration. Has no dependency on other functional modules — every other module may
 * depend on it, never the other way round.
 */
package com.cas.tsas.common;
