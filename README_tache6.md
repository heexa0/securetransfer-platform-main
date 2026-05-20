# Tâche 6 — Agence & Opérations

**Développeur** : Hiba  
**Module** : `agency`  
**Branche** : `tache6-hiba`

---

## Description

Module de gestion des agences et opérations financières (retraits, dépôts, commissions) avec calcul précis en BigDecimal, validation des transactions, et rapport journalier.

---

## Structure du module
platform/src/main/java/com/securetransfer/platform/agency/
├── controller/
│   └── AgencyController.java
├── dto/
│   ├── AgencyOperationRequest.java
│   └── AgencyOperationResponse.java
├── entity/
│   ├── Agency.java
│   └── AgencyOperation.java
├── repository/
│   └── AgencyRepository.java
└── service/
├── AgencyOperationService.java
└── CommissionCalculationService.java

---

## Ce qui a été réalisé

### Entités JPA
- **Agency** : entité agence avec code unique, ville, solde, taux de commission, statut actif
- **AgencyOperation** : entité opération avec type (DEPOSIT, WITHDRAWAL, TRANSFER, COMMISSION), montant, commission, code de validation unique, statut (PENDING, COMPLETED, FAILED, CANCELLED)

### Services
- **CommissionCalculationService** : calcul de commission avec Stream API Java 21, groupement par type, calcul journalier
- **AgencyOperationService** : création d'opérations avec génération automatique de code de validation (VAL-XXXXXXXX), vérification du solde pour les retraits, vérification que l'agence est active, rapport journalier

### Repository
- **AgencyRepository** : requêtes JPQL avec `@Query`, pagination avec `Pageable`, recherche par plage de dates, calcul du total des montants par agence

### Controller REST
| Méthode | Endpoint | Description | Rôle requis |
|---------|----------|-------------|-------------|
| POST | `/api/agency/operation` | Créer une opération | ADMIN, AGENCY |
| GET | `/api/agency/{id}/operations` | Lister les opérations (paginé) | ADMIN, AGENCY |
| GET | `/api/agency/{id}/daily-report` | Rapport journalier | ADMIN, AGENCY |

### Migration Flyway
- **V3__agency.sql** : création des tables `agencies` et `agency_operations` avec index sur `agency_id` et `created_at`

### Tests unitaires
- **CommissionCalculationServiceTest** : 3 tests (calcul commission, total commissions, commission journalière)
- **AgencyOperationServiceTest** : 4 tests (dépôt, retrait solde insuffisant, agence introuvable, agence inactive)

---

## Résultats des tests
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
---

## Technologies utilisées

- Java 21 (Stream API, records)
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL 16
- Flyway (migration)
- BigDecimal (calculs précis)
- JUnit 5 + Mockito (tests)
- Docker (PostgreSQL + Redis)