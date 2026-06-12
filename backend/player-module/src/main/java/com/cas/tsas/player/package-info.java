/**
 * Player module — management of player profiles (name, gender, ranking, handedness,
 * backhand type): CRUD operations and search.
 *
 * <p>Structured along Clean Architecture layers:
 * <ul>
 *   <li>{@code domain.model} / {@code domain.exception} — framework-free enterprise rules</li>
 *   <li>{@code application.port.in} / {@code application.port.out} / {@code application.service} — use cases and ports</li>
 *   <li>{@code infrastructure.web} / {@code infrastructure.persistence} — REST and JPA adapters</li>
 * </ul>
 * Dependencies point inward (infrastructure → application → domain). Module dependencies:
 * {@code common-module} only. Other modules reach player functionality through its
 * application-layer ports (e.g. {@code LoadPlayerPort}).
 */
package com.cas.tsas.player;
