# 📦 Module ING2 — Gestion des Utilisateurs (Users)
**Projet SecureTransfer — Branche : `feature/ing2-users`**

> Ce module gère la création, l'authentification et la gestion des profils utilisateurs (Particuliers, Agences, Entreprises). Il s'appuie sur le module ING1 (Auth/JWT) et expose ses services aux modules ING3, ING4 et ING6.

---

## 👤 Auteur
- **Module** : ING2 — User Management
- **Dépend de** : ING1 (Auth, JWT, SecurityConfig)
- **Expose vers** : ING3 (Transferts), ING4 (KYC), ING6 (Notifications)

---

## 📋 Table des matières

1. [Architecture du module](#1-architecture-du-module)
2. [Prérequis et installation](#2-prérequis-et-installation)
3. [Configuration](#3-configuration)
4. [Structure des packages](#4-structure-des-packages)
5. [Entités JPA](#5-entités-jpa)
6. [DTOs](#6-dtos)
7. [Repositories](#7-repositories)
8. [UserMapper (MapStruct)](#8-usermapper-mapstruct)
9. [UserService — API interne](#9-userservice--api-interne)
10. [UserController — API REST](#10-usercontroller--api-rest)
11. [Sécurité](#11-sécurité)
12. [Migration SQL (Flyway)](#12-migration-sql-flyway)
13. [Tests](#13-tests)
14. [Validation des endpoints](#14-validation-des-endpoints)
15. [Ce que vous exposez aux autres modules](#15-ce-que-vous-exposez-aux-autres-modules)
16. [Erreurs connues et solutions](#16-erreurs-connues-et-solutions)

---

## 1. Architecture du module

```
com.securetransfer.platform/
│
├── [ING1 — NE PAS MODIFIER]
│   ├── config/          ← JwtAuthFilter, SecurityConfig (modifié légèrement)
│   ├── controller/      ← AuthController
│   ├── dto/             ← LoginRequest, RegisterRequest, AuthResponse
│   ├── entity/          ← UserCredential
│   ├── repository/      ← UserRepository
│   ├── security/        ← JwtService, MfaService
│   └── service/         ← AuthService, UserDetailsServiceImpl
│
├── [ING2 — VOTRE MODULE]
│   ├── user/
│   │   ├── entity/      ← BaseUser, Particulier, Agence, Entreprise, Role, Permission, KycStatus
│   │   ├── repository/  ← ParticulierRepository, AgenceRepository, EntrepriseRepository, RoleRepository
│   │   ├── dto/         ← CreateParticulierRequest, CreateAgenceRequest, CreateEntrepriseRequest,
│   │   │                   UpdateUserRequest, UserResponse
│   │   ├── mapper/      ← UserMapper (MapStruct)
│   │   ├── service/     ← UserService
│   │   └── controller/  ← UserController
│   │
│   └── common/
│       ├── util/        ← EncryptedStringConverter
│       └── exception/   ← BusinessException, ResourceNotFoundException
│
└── resources/
    └── db/migration/
        └── V2__users.sql   ← Migration Flyway ING2
```

---

## 2. Prérequis et installation

### Versions requises
| Outil | Version |
|-------|---------|
| Java | 21+ |
| Spring Boot | 3.5.x |
| PostgreSQL | 16.x |
| Redis | 7.x |
| Maven | 3.9+ |

### Dépendances ajoutées au `pom.xml`

> ⚠️ Ces dépendances **n'étaient pas dans le projet ING1** — elles ont été ajoutées pour ING2.

```xml
<!-- Dans <dependencies> -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>

<!-- Dans <annotationProcessorPaths> de maven-compiler-plugin -->
<!-- (pour les deux executions : default-compile ET default-testCompile) -->
<path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
</path>
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>0.2.0</version>
</path>
```

> **Pourquoi ?** MapStruct génère automatiquement le code de conversion Entité ↔ DTO à la compilation. Le `lombok-mapstruct-binding` évite les conflits entre Lombok et MapStruct.

### Lancer le projet

```bash
# 1. Démarrer Docker (PostgreSQL + Redis)
docker-compose up -d

# 2. Vérifier que les conteneurs tournent
docker ps
# Doit afficher 2 conteneurs : postgres et redis

# 3. Lancer l'application dans IntelliJ
# Cliquer sur ▶️ à côté de PlatformApplication.main()

# 4. Vérifier dans les logs :
# Tomcat started on port 8080
# Started PlatformApplication in X seconds
```

---

## 3. Configuration

### `application.yml` — Ajout ING2

```yaml
# Ajouter à la FIN du fichier application.yml
# IMPORTANT : encryption: doit être au niveau racine (pas indenté sous jwt:)

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 900000
  refresh-expiration: 604800000

encryption:           # ← au même niveau que jwt:, PAS en dessous
  key: 12345678901234567890123456789012   # 32 caractères exactement (AES-256)
```

> ⚠️ **Erreur fréquente** : Si `encryption:` est indenté sous `jwt:`, Spring Boot ne trouve pas la clé et l'application crashe au démarrage avec `Could not resolve placeholder 'encryption.key'`.

### `.gitignore`

```
# Ajouter si un fichier .env est utilisé
.env
```

---

## 4. Structure des packages

### Comment créer les packages dans IntelliJ

**Clic droit sur `com.securetransfer.platform`** → New → Package

| Package à créer | Contenu |
|---|---|
| `user.entity` | Entités JPA |
| `user.repository` | Interfaces Spring Data JPA |
| `user.dto` | Records Java (requêtes/réponses) |
| `user.mapper` | Interface MapStruct |
| `user.service` | Logique métier |
| `user.controller` | Endpoints REST |
| `common.util` | Utilitaires (chiffrement) |
| `common.exception` | Exceptions personnalisées |

---

## 5. Entités JPA

### `KycStatus.java` — Enum

```java
package com.securetransfer.platform.user.entity;

public enum KycStatus {
    PENDING,    // En attente de vérification
    VERIFIED,   // KYC validé
    REJECTED    // KYC rejeté
}
```

### `Role.java`

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // ex: "ROLE_USER", "ROLE_ADMIN", "ROLE_AGENCE"

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
}
```

### `Permission.java`

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter @Setter
@NoArgsConstructor
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // ex: "TRANSFER_CREATE", "KYC_UPDATE"
}
```

### `BaseUser.java` — Classe mère abstraite

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "daily_transaction_limit", precision = 15, scale = 2)
    private java.math.BigDecimal dailyTransactionLimit;

    @Column(name = "single_transaction_limit", precision = 15, scale = 2)
    private java.math.BigDecimal singleTransactionLimit;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### `Particulier.java`

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "particuliers")
@Getter @Setter
@NoArgsConstructor
public class Particulier extends BaseUser {

    private String cin;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String nationality;
}
```

### `Agence.java`

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agences")
@Getter @Setter
@NoArgsConstructor
public class Agence extends BaseUser {

    @Column(name = "nom_agence")
    private String nomAgence;

    @Column(name = "code_agence", unique = true)
    private String codeAgence;

    private String adresse;
}
```

### `Entreprise.java`

```java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entreprises")
@Getter @Setter
@NoArgsConstructor
public class Entreprise extends BaseUser {

    @Column(name = "raison_sociale")
    private String raisonSociale;

    @Column(unique = true)
    private String siret;

    private String adresse;
}
```

---

## 6. DTOs

### `UserResponse.java` — Réponse universelle

```java
package com.securetransfer.platform.user.dto;

import com.securetransfer.platform.user.entity.KycStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String email,
    KycStatus kycStatus,
    BigDecimal dailyTransactionLimit,
    BigDecimal singleTransactionLimit,
    LocalDateTime createdAt
) {}
```

### `CreateParticulierRequest.java`

```java
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.*;

public record CreateParticulierRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    String phoneNumber,
    String cin,
    String dateOfBirth,
    String nationality
) {}
```

### `CreateAgenceRequest.java`

```java
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.*;

public record CreateAgenceRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    String phoneNumber,
    String nomAgence,
    String codeAgence,
    String adresse
) {}
```

### `CreateEntrepriseRequest.java`

```java
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.*;

public record CreateEntrepriseRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    String phoneNumber,
    String raisonSociale,
    String siret,
    String adresse
) {}
```

### `UpdateUserRequest.java`

```java
package com.securetransfer.platform.user.dto;

public record UpdateUserRequest(
    String phoneNumber,
    String adresse
) {}
```

---

## 7. Repositories

```java
// ParticulierRepository.java
@Repository
public interface ParticulierRepository extends JpaRepository<Particulier, Long> {
    Optional<Particulier> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
        SELECT p FROM Particulier p
        WHERE (:kycStatus IS NULL OR p.kycStatus = :kycStatus)
        ORDER BY p.createdAt DESC
        """)
    Page<Particulier> findAllFiltered(@Param("kycStatus") KycStatus kycStatus, Pageable pageable);
}
```

> **Répéter le même pattern** pour `AgenceRepository` et `EntrepriseRepository` en remplaçant `Particulier` par `Agence` ou `Entreprise`.

```java
// RoleRepository.java
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
```

---

## 8. UserMapper (MapStruct)

```java
package com.securetransfer.platform.user.mapper;

import com.securetransfer.platform.user.dto.*;
import com.securetransfer.platform.user.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(Particulier particulier);
    UserResponse toResponse(Agence agence);
    UserResponse toResponse(Entreprise entreprise);

    Particulier toParticulier(CreateParticulierRequest request);
    Agence toAgence(CreateAgenceRequest request);
    Entreprise toEntreprise(CreateEntrepriseRequest request);

    void updateParticulierFromRequest(UpdateUserRequest request,
                                      @MappingTarget Particulier target);
}
```

> ⚠️ **Important** : Après avoir créé `UserMapper.java`, faire **Build → Rebuild Project**. MapStruct génère automatiquement `UserMapperImpl.java` dans `target/generated-sources/`.

> ⚠️ **Si IntelliJ ne trouve pas MapStruct** : aller dans **File → Settings → Build → Compiler → Annotation Processors** et cocher **Enable annotation processing**.

---

## 9. UserService — API interne

Le `UserService` est **l'interface principale** que les autres modules (ING3, ING4, ING6) vont utiliser.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final ParticulierRepository particulierRepository;
    private final AgenceRepository agenceRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ── Méthodes disponibles pour les autres modules ──────────────

    public UserResponse createParticulier(CreateParticulierRequest request) { ... }
    public UserResponse createAgence(CreateAgenceRequest request) { ... }
    public UserResponse createEntreprise(CreateEntrepriseRequest request) { ... }

    public UserResponse getUser(Long id) { ... }
    public UserResponse getUserByEmail(String email) { ... }

    public Page<UserResponse> getAllUsers(KycStatus filter, int page, int size) { ... }

    // ← ING3 UTILISE CETTE MÉTHODE
    public void validateTransactionLimit(Long userId, BigDecimal amount) { ... }

    // ← ING4 UTILISE CETTE MÉTHODE
    public void updateKycStatus(Long userId, KycStatus newStatus) { ... }
}
```

---

## 10. UserController — API REST

### Endpoints disponibles

| Méthode | URL | Auth requise | Description |
|---------|-----|-------------|-------------|
| `POST` | `/api/users/particuliers` | ❌ Aucune | Créer un particulier |
| `POST` | `/api/users/agences` | ✅ `ROLE_ADMIN` | Créer une agence |
| `GET` | `/api/users/me` | ✅ JWT valide | Mon propre profil |
| `GET` | `/api/users/{id}` | ✅ ADMIN ou propriétaire | Profil par ID |
| `GET` | `/api/users` | ✅ `ROLE_ADMIN` | Liste paginée |
| `PATCH` | `/api/users/{id}/kyc` | ✅ `ROLE_ADMIN` | Mettre à jour KYC |

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/particuliers")
    public ResponseEntity<UserResponse> createParticulier(
            @Valid @RequestBody CreateParticulierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createParticulier(request));
    }

    @PostMapping("/agences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createAgence(
            @Valid @RequestBody CreateAgenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createAgence(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getUserByEmail(principal.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAll(
            @RequestParam(required = false) KycStatus kycStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getAllUsers(kycStatus, page, size));
    }

    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateKyc(
            @PathVariable Long id,
            @RequestParam KycStatus status) {
        userService.updateKycStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 11. Sécurité

### Modification de `SecurityConfig.java` (ING1)

> ⚠️ **Ne modifier QUE la liste des endpoints publics** — ne pas toucher au reste.

```java
// Dans SecurityConfig.java — SEULE MODIFICATION ING2
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/mfa/**",
        "/api/users/particuliers",  // ← AJOUTÉ PAR ING2
        "/swagger-ui/**",
        "/v3/api-docs/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

### `EncryptedStringConverter.java`

```java
package com.securetransfer.platform.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Value("${encryption.key}")
    private String encryptionKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Erreur chiffrement", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Erreur déchiffrement", e);
        }
    }
}
```

---

## 12. Migration SQL (Flyway)

Fichier : `src/main/resources/db/migration/V2__users.sql`

```sql
-- Roles et permissions
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles(id),
    permission_id BIGINT REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- Particuliers
CREATE TABLE IF NOT EXISTS particuliers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    daily_transaction_limit DECIMAL(15,2),
    single_transaction_limit DECIMAL(15,2),
    cin VARCHAR(20),
    date_of_birth DATE,
    nationality VARCHAR(100),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Agences
CREATE TABLE IF NOT EXISTS agences (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    daily_transaction_limit DECIMAL(15,2),
    single_transaction_limit DECIMAL(15,2),
    nom_agence VARCHAR(255),
    code_agence VARCHAR(50) UNIQUE,
    adresse TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Entreprises
CREATE TABLE IF NOT EXISTS entreprises (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    daily_transaction_limit DECIMAL(15,2),
    single_transaction_limit DECIMAL(15,2),
    raison_sociale VARCHAR(255),
    siret VARCHAR(50) UNIQUE,
    adresse TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Table de jointure user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Données initiales
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_AGENCE') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ENTREPRISE') ON CONFLICT DO NOTHING;
```

> **Vérification Flyway** : Dans les logs au démarrage, chercher :
> ```
> Successfully applied 1 migration to schema "public", now at version v2
> ```

---

## 13. Tests

Fichier : `src/test/java/com/securetransfer/platform/user/UserServiceTest.java`

### Résultats des tests ✅

```
✅ createParticulier — création réussie → retourne UserResponse    (1 sec 502 ms)
✅ validateTransactionLimit — montant > limite → BusinessException  (11 ms)
✅ createParticulier — email déjà existant → BusinessException      (6 ms)

BUILD SUCCESS — 3 tests passés
```

### Lancer les tests

```bash
# Dans IntelliJ : clic droit sur UserServiceTest.java → Run
# Ou en ligne de commande :
mvn test -Dtest=UserServiceTest
```

---

## 14. Validation des endpoints

### Étape 1 — Démarrer le serveur

Lancer `PlatformApplication` dans IntelliJ. Attendre :
```
Tomcat started on port 8080 (http)
Started PlatformApplication in X seconds
```

### Étape 2 — Créer un particulier (endpoint public)

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/users/particuliers" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"ahmed@test.com","password":"motdepasse123","phoneNumber":"+212612345678","cin":"AB123456","dateOfBirth":"1995-03-15","nationality":"Marocain"}'
```

**Réponse attendue (HTTP 201) :**
```
id                     : 1
email                  : ahmed@test.com
kycStatus              : PENDING
dailyTransactionLimit  : 10000
singleTransactionLimit : 2000
createdAt              : 2026-05-17T00:33:38.659982
```

### Étape 3 — Créer un compte auth (ING1) et obtenir un token

```powershell
# Register dans le système auth ING1
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"ahmed@test.com","password":"motdepasse123"}'

# Login → obtenir le token JWT
$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"ahmed@test.com","password":"motdepasse123"}'

$token = $response.token
Write-Host "Token:" $token
```

**Réponse attendue :**
```
Token: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaG1lZEB0...
```

### Étape 4 — GET /api/users/me (endpoint protégé)

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/users/me" `
  -Method GET `
  -Headers @{Authorization="Bearer $token"}
```

**Réponse attendue (HTTP 200) :**
```
id                     : 1
email                  : ahmed@test.com
kycStatus              : PENDING
dailyTransactionLimit  : 10000.00
singleTransactionLimit : 2000.00
createdAt              : 2026-05-17T00:33:38.659982
```

### Étape 5 — GET /api/users/{id}

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/users/1" `
  -Method GET `
  -Headers @{Authorization="Bearer $token"}
```

---

## 15. Ce que vous exposez aux autres modules

### Pour ING3 — Transferts

```java
// Injecter UserService dans votre service ING3
@Autowired
private UserService userService;

// Vérifier la limite avant d'effectuer un transfert
userService.validateTransactionLimit(userId, montant);
// Lance BusinessException si montant > singleTransactionLimit
```

### Pour ING4 — KYC

```java
// Après validation KYC, mettre à jour le statut
userService.updateKycStatus(userId, KycStatus.VERIFIED);
// ou
userService.updateKycStatus(userId, KycStatus.REJECTED);
```

### Pour ING3, ING4, ING6 — Récupérer un profil

```java
// Par ID
UserResponse user = userService.getUser(Long id);

// Par email (utile pour récupérer l'utilisateur connecté)
UserResponse user = userService.getUserByEmail(String email);

// Liste paginée avec filtre KYC
Page<UserResponse> users = userService.getAllUsers(KycStatus.PENDING, 0, 20);
```

### UserResponse — Structure retournée

```java
public record UserResponse(
    Long id,
    String email,
    KycStatus kycStatus,           // PENDING | VERIFIED | REJECTED
    BigDecimal dailyTransactionLimit,
    BigDecimal singleTransactionLimit,
    LocalDateTime createdAt
) {}
```

### ⚠️ Règles de collaboration inter-modules

1. **Ne jamais injecter directement** `ParticulierRepository`, `AgenceRepository` ou `EntrepriseRepository` depuis un autre module. Utiliser uniquement `UserService`.
2. **Ne jamais modifier** les classes du module ING1 sans concertation avec l'équipe ING1.
3. **Communiquer via l'interface** `UserService` — les autres modules n'ont pas besoin de connaître les entités internes.

---

## 16. Erreurs connues et solutions

| Erreur | Cause | Solution |
|--------|-------|----------|
| `Could not resolve placeholder 'encryption.key'` | `encryption:` mal indenté dans `application.yml` | Mettre `encryption:` au niveau racine, pas sous `jwt:` |
| `package org.mapstruct does not exist` | MapStruct processor absent du `pom.xml` | Ajouter `mapstruct-processor` dans `<annotationProcessorPaths>` |
| `HTTP 403` sur `/api/v1/auth/login` | L'email n'existe pas dans `user_credentials` | Faire d'abord `/api/v1/auth/register` |
| `HTTP 403` sur `/api/users/me` | L'email JWT n'existe pas dans `particuliers` | Utiliser le même email pour register ING1 ET créer particulier ING2 |
| `HTTP 403` sur `/api/v1/auth/register` | Email déjà utilisé → exception → Spring Security intercepte | Utiliser un email différent |
| `cannot find symbol method createAgence` | Méthode absente dans `UserService` | Ajouter `createAgence()` et `createEntreprise()` |
| `Build failed: Annotation Processing` | IntelliJ ne traite pas les annotations | File → Settings → Compiler → Annotation Processors → Enable |

---

## ✅ Checklist de validation complète

### Jour 1
- [ ] Docker : `docker ps` → 2 conteneurs Up (postgres + redis)
- [ ] `pom.xml` : MapStruct ajouté + processor configuré
- [ ] `PlatformApplication.java` : `@EnableJpaAuditing` présent
- [ ] Packages créés : `user/`, `common/`
- [ ] `BusinessException.java` et `ResourceNotFoundException.java` créés
- [ ] `KycStatus.java` créé
- [ ] `Role.java` et `Permission.java` créés avec `@Entity`
- [ ] `BaseUser.java` créé avec `@MappedSuperclass`
- [ ] `Particulier.java`, `Agence.java`, `Entreprise.java` créés
- [ ] `EncryptedStringConverter.java` créé
- [ ] `V2__users.sql` créé → logs Flyway : "2 migrations applied"
- [ ] `application.yml` : `encryption.key` ajouté au niveau racine

### Jour 2
- [ ] `RoleRepository`, `ParticulierRepository`, `AgenceRepository`, `EntrepriseRepository` créés
- [ ] DTOs créés : `CreateParticulierRequest`, `CreateAgenceRequest`, `CreateEntrepriseRequest`, `UpdateUserRequest`, `UserResponse`
- [ ] `UserMapper.java` créé → Rebuild → `target/generated-sources/` contient `UserMapperImpl`
- [ ] `UserService.java` créé avec toutes les méthodes

### Jour 3
- [ ] `UserController.java` créé avec tous les endpoints
- [ ] `SecurityConfig.java` modifié : `/api/users/particuliers` en public
- [ ] Tests : `mvn test` → 3 tests verts ✅
- [ ] `POST /api/users/particuliers` → HTTP 201 ✅
- [ ] `GET /api/users/me` avec token JWT → HTTP 200 ✅
- [ ] `git push origin feature/ing2-users` ✅

---

*README généré pour le module ING2 — SecureTransfer Platform*
*Dernière mise à jour : Mai 2026*
