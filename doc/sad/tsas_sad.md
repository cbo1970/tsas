---
date: 24. Feb 2026
title: "Software Arichitecure Document TSAS Tennis Score and Statisik"
---

# Einführung und Ziele 
Für die geziehlte Vorbereitung auf ein Tennismatch fehlt heute eine App, welche es Eltern und Trainer ermöglicht Statistiken und Angaben über die Spielweise und Eigenarten des eigenen Spielers sowie die des Gegners zu erlangen. Dies soll die App <b>Tennis Score and Statistic</b>, kurz TSaS beheben. Ziel ist es eine Web-App, später auch iOS App zu erstellen, mit denen die Trainer oder Eltern einen Match Punkt für Punkt mit fixen und freien Angaben dokumentieren können.

## Stakeholder
- Eltern
- Tennistrainer


## Version 1 (MVP):

Es soll eine Web-Applikation erstellt werden mit der, der aktuelle Spielstand festgehalten werden kann
Jeder Punkt kann mit fixen Attributen wie 

- Forhand winner
- Backhand winner
- 1. Service fault
- 1. Service winner
- Ass
- out long
- out side
- net
- etc. 

Dokumentiert werden. Dies sowohl für den eigenen Spieler wie auch für den Gegner.
Die Daten werden in einer Datenbank festgehalten
Die Spieler werden mit zusätzlichen Attributen versehen wie:

- Name Vorname
- Geschlecht
- Ranking
- Linkshänder, Rechtshänder
- Single handed Backhand / Double handed Backhand

Einfache Statiskiken wie:
- Head to Head
- % of Winner
- % unforced Error
- % first Service
- % second Service
- number of Double faults / asses


### Authorisierung

Der Benutzer muss sich in TSaS Registrieen und vor der Benutzung authorisieren.

## Version 2
- Authentifizierung / Authorisierung kann auch über Google als IDP erfolgen
- Erweiterte statistische Auswertungen
- Native iOS Frontend für iPad

## Version 3
- Aufsprungpunkte des Balles kann mittels Touch auf einem Skizzierten Tennisfels im UI festgehalten werden.

## Version 4
- Zugriffe auf das API von Swisstennis (wenn möglich muss mit Swisstennis abgeklärt werden)

## Version 5
- Anschluss einer Kammera um die Aufsprungpunkte automatisch zu erfassen. "Hawk Eye very light"


## Funktionale Anforderungen
- Registrieren eines neune Users
- Authentifizierung / Authorisierung des Users mittels OAuth2
- Erfassen eines Spielers
- Suchen eines Spielers
- Erstellen einer Begegnung (Matches)
- Erfassen Attribute der Begegnung ( Anzahl Gewinnsätze, Match T-Break, Short Set) 
- Erfassen des Spielstandes
- Erfassen der fixen Attribute
- Erfassen von freien Bemerkungen (Textfeld)
- Erstellen Statistik Head to Head


## Qualitätsattribute
- Wart - und Erweiterbar
- Verfügbarkeit von 95%
- Anzahl gleichzeitiger Benutzer 100 für die max. Response Time 
- Erfassen der Daten max. Response Time 0.250 Sec
- Erstellen der Statistik max. Response Time 1min 


## Techstack 
- Spring Boot
- Keyclock
- Angualar
- Postgres
- für iOS Swift



