package com.cas.tsas.auth.application.port.in;

import com.cas.tsas.auth.domain.CurrentUser;

/** Liefert den aktuell authentifizierten Nutzer. */
public interface CurrentUserProvider {
    CurrentUser get();
}
