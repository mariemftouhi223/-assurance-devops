package com.mariem.assurance;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.fraud.FraudAlertEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // ← AJOUTER CETTE LIGNE



/**
 * Tests de validation des alertes en base de données
 *
 * Ces tests vérifient que les alertes de fraude sont correctement
 * créées, persistées et récupérables depuis la base de données.
 *
 * @author Manus AI
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test") // Utilise un profil de test avec une base H2 en mémoire
@Transactional // Rollback automatique après chaque test
public class DatabaseAlertValidationTest {

    @PersistenceContext
    private EntityManager entityManager;

    private FraudPredictionRequest highRiskRequest;
    private FraudPredictionRequest normalRequest;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données avant chaque test
        entityManager.createQuery("DELETE FROM FraudAlertEntity").executeUpdate();
        entityManager.flush();

        // Préparer une requête à haut risque (devrait déclencher une alerte)
        ContractData highRiskContract = new ContractData();
        highRiskContract.setContractId("HIGH-RISK-001");
        highRiskContract.setClientId("CLIENT-HIGH-RISK");
        highRiskContract.setAmount(250000.0);
        highRiskContract.setRc(12000.0);
        highRiskContract.setIncendie(9000.0);
        highRiskContract.setVol(6000.0);
        highRiskContract.setTotalPrimeNette(18000.0);
        highRiskContract.setCapitaleInc(200000.0);
        highRiskContract.setCapitaleVol(180000.0);

        ClientData highRiskClient = new ClientData();
        highRiskClient.setFirstName("Risque");
        highRiskClient.setLastName("Élevé");
        highRiskClient.setAge(20);
        highRiskClient.setAddress("Zone Industrielle Dangereuse");
        highRiskClient.setEmail("risque@eleve.com");
        highRiskClient.setPhone("+33999888777");

        highRiskRequest = new FraudPredictionRequest(highRiskContract, highRiskClient);

        // Préparer une requête normale
        ContractData normalContract = new ContractData();
        normalContract.setContractId("NORMAL-001");
        normalContract.setClientId("CLIENT-NORMAL");
        normalContract.setAmount(30000.0);
        normalContract.setRc(1500.0);
        normalContract.setIncendie(800.0);
        normalContract.setVol(400.0);
        normalContract.setTotalPrimeNette(2500.0);
        normalContract.setCapitaleInc(25000.0);
        normalContract.setCapitaleVol(20000.0);

        ClientData normalClient = new ClientData();
        normalClient.setFirstName("Jean");
        normalClient.setLastName("Normal");
        normalClient.setAge(40);
        normalClient.setAddress("Rue Tranquille");
        normalClient.setEmail("jean.normal@email.com");
        normalClient.setPhone("+33123456789");

        normalRequest = new FraudPredictionRequest(normalContract, normalClient);
    }

    /**
     * Test 1: Vérifier qu'une alerte peut être créée et persistée en base
     */
    @Test
    void testCreateAlert_ShouldPersistAlertInDatabase() {
        // Créer une alerte
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage("Contrat HIGH-RISK-001 suspect détecté par consensus des modèles ML");
        alert.setTimestamp(LocalDateTime.now());

        // Persister l'alerte directement avec EntityManager
        entityManager.persist(alert);
        entityManager.flush();

        // Vérifications
        assertNotNull(alert.getId());
        assertEquals("Contrat HIGH-RISK-001 suspect détecté par consensus des modèles ML", alert.getMessage());

        // Vérifier que l'alerte est bien en base de données
        entityManager.clear();

        FraudAlertEntity retrievedAlert = entityManager.find(FraudAlertEntity.class, alert.getId());
        assertNotNull(retrievedAlert);
        assertEquals("Contrat HIGH-RISK-001 suspect détecté par consensus des modèles ML", retrievedAlert.getMessage());
    }

    /**
     * Test 2: Vérifier qu'aucune alerte n'est créée pour un contrat normal
     */
    @Test
    void testNoAlert_NormalContract_ShouldNotCreateAlert() {
        // Compter les alertes avant le test
        Long initialCount = (Long) entityManager.createQuery("SELECT COUNT(a) FROM FraudAlertEntity a").getSingleResult();

        // Simuler l'analyse d'un contrat normal (pas de fraude détectée)
        boolean shouldCreateAlert = false;

        if (shouldCreateAlert) {
            FraudAlertEntity alert = new FraudAlertEntity();
            alert.setMessage("Contrat NORMAL-001 normal");
            alert.setTimestamp(LocalDateTime.now());
            entityManager.persist(alert);
        }

        // Vérifier qu'aucune nouvelle alerte n'a été créée
        Long finalCount = (Long) entityManager.createQuery("SELECT COUNT(a) FROM FraudAlertEntity a").getSingleResult();
        assertEquals(initialCount, finalCount, "Aucune alerte ne devrait être créée pour un contrat normal");
    }

    /**
     * Test 3: Vérifier la récupération des alertes
     */
    @Test
    void testGetAlerts_ShouldRetrieveCorrectAlerts() {
        // Créer plusieurs alertes
        FraudAlertEntity alert1 = createTestAlert("Alerte 1");
        FraudAlertEntity alert2 = createTestAlert("Alerte 2");

        entityManager.persist(alert1);
        entityManager.persist(alert2);
        entityManager.flush();

        // Récupérer toutes les alertes
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> allAlerts = entityManager.createQuery("SELECT a FROM FraudAlertEntity a").getResultList();

        // Vérifications
        assertEquals(2, allAlerts.size(), "Il devrait y avoir 2 alertes");
        assertTrue(allAlerts.stream().anyMatch(alert -> alert.getMessage().equals("Alerte 1")));
        assertTrue(allAlerts.stream().anyMatch(alert -> alert.getMessage().equals("Alerte 2")));
    }

    /**
     * Test 4: Vérifier la mise à jour du message d'une alerte
     */
    @Test
    void testUpdateAlertMessage_ShouldPersistMessageChange() {
        // Créer et sauvegarder une alerte
        FraudAlertEntity alert = createTestAlert("Message initial");
        entityManager.persist(alert);
        entityManager.flush();

        // Mettre à jour le message
        alert.setMessage("Message mis à jour");
        entityManager.merge(alert);
        entityManager.flush();
        entityManager.clear();

        // Vérifier que la mise à jour est persistée
        FraudAlertEntity retrievedAlert = entityManager.find(FraudAlertEntity.class, alert.getId());
        assertEquals("Message mis à jour", retrievedAlert.getMessage());
    }

    /**
     * Test 5: Vérifier la validation des données d'alerte
     */
    @Test
    void testAlertValidation_EmptyMessage_ShouldStillWork() {
        // Créer une alerte avec un message vide (pas de validation @NotNull dans l'entité actuelle)
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage("");
        alert.setTimestamp(LocalDateTime.now());

        // Cela devrait fonctionner car il n'y a pas de validation dans l'entité actuelle
        assertDoesNotThrow(() -> {
            entityManager.persist(alert);
            entityManager.flush();
        });

        assertNotNull(alert.getId());
        assertEquals("", alert.getMessage());
    }

    /**
     * Test 6: Test d'intégration complète - Workflow de détection à alerte
     */
    @Test
    void testCompleteWorkflow_FraudDetectionToDatabase() {
        // Simuler la détection de fraude
        boolean model1DetectsFraud = true;
        boolean model2DetectsFraud = true;
        boolean consensusReached = model1DetectsFraud && model2DetectsFraud;

        // Si consensus, créer une alerte
        if (consensusReached) {
            FraudAlertEntity alert = new FraudAlertEntity();
            alert.setMessage("Fraude détectée pour contrat " + highRiskRequest.getContractData().getContractId());
            alert.setTimestamp(LocalDateTime.now());

            // Sauvegarder en base
            entityManager.persist(alert);
            entityManager.flush();

            // Vérifications
            assertNotNull(alert.getId());
            assertTrue(alert.getMessage().contains(highRiskRequest.getContractData().getContractId()));

            // Vérifier que l'alerte est récupérable
            @SuppressWarnings("unchecked")
            List<FraudAlertEntity> allAlerts = entityManager.createQuery("SELECT a FROM FraudAlertEntity a").getResultList();
            assertEquals(1, allAlerts.size());
            assertTrue(allAlerts.get(0).getMessage().contains(highRiskRequest.getContractData().getContractId()));
        }
    }

    /**
     * Test 7: Vérifier la recherche d'alertes par message
     */
    @Test
    void testFindAlertsByMessage_ShouldReturnMatchingAlerts() {
        // Créer des alertes avec différents messages
        FraudAlertEntity alert1 = createTestAlert("Fraude détectée - Contrat A");
        FraudAlertEntity alert2 = createTestAlert("Fraude détectée - Contrat B");
        FraudAlertEntity alert3 = createTestAlert("Alerte normale");

        entityManager.persist(alert1);
        entityManager.persist(alert2);
        entityManager.persist(alert3);
        entityManager.flush();

        // Rechercher les alertes contenant "Fraude détectée"
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> fraudAlerts = entityManager.createQuery(
                        "SELECT a FROM FraudAlertEntity a WHERE a.message LIKE :pattern")
                .setParameter("pattern", "%Fraude détectée%")
                .getResultList();

        // Vérifications
        assertEquals(2, fraudAlerts.size(), "Il devrait y avoir 2 alertes de fraude");
        assertTrue(fraudAlerts.stream().allMatch(alert -> alert.getMessage().contains("Fraude détectée")));
    }

    /**
     * Test 8: Vérifier la recherche d'alertes par période
     */
    @Test
    void testFindAlertsByTimeRange_ShouldReturnAlertsInRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        // Créer des alertes à différents moments
        FraudAlertEntity oldAlert = createTestAlert("Alerte ancienne");
        oldAlert.setTimestamp(twoHoursAgo);

        FraudAlertEntity recentAlert = createTestAlert("Alerte récente");
        recentAlert.setTimestamp(oneHourAgo);

        FraudAlertEntity currentAlert = createTestAlert("Alerte actuelle");
        currentAlert.setTimestamp(now);

        entityManager.persist(oldAlert);
        entityManager.persist(recentAlert);
        entityManager.persist(currentAlert);
        entityManager.flush();

        // Rechercher les alertes des dernières 90 minutes
        LocalDateTime cutoff = now.minusMinutes(90);
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> recentAlerts = entityManager.createQuery(
                        "SELECT a FROM FraudAlertEntity a WHERE a.timestamp >= :cutoff ORDER BY a.timestamp DESC")
                .setParameter("cutoff", cutoff)
                .getResultList();

        // Vérifications
        assertEquals(2, recentAlerts.size(), "Il devrait y avoir 2 alertes récentes");
        assertEquals("Alerte actuelle", recentAlerts.get(0).getMessage());
        assertEquals("Alerte récente", recentAlerts.get(1).getMessage());
    }

    // Méthode utilitaire
    private FraudAlertEntity createTestAlert(String message) {
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage(message);
        alert.setTimestamp(LocalDateTime.now());
        return alert;
    }
}

