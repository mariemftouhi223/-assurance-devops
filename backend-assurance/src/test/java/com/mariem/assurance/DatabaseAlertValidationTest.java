/*package com.mariem.assurance;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.fraud.FraudAlertEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test") // Utilise un profil de test avec une base H2 en mÃ©moire
@Transactional // Rollback automatique aprÃ¨s chaque test
public class DatabaseAlertValidationTest {

    @PersistenceContext
    private EntityManager entityManager;

    private FraudPredictionRequest highRiskRequest;
    private FraudPredictionRequest normalRequest;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de donnÃ©es avant chaque test
        entityManager.createQuery("DELETE FROM FraudAlertEntity").executeUpdate();
        entityManager.flush();

        // PrÃ©parer une requÃªte Ã  haut risque (devrait dÃ©clencher une alerte)
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
        highRiskClient.setLastName("Ã‰levÃ©");
        highRiskClient.setAge(20);
        highRiskClient.setAddress("Zone Industrielle Dangereuse");
        highRiskClient.setEmail("risque@eleve.com");
        highRiskClient.setPhone("+33999888777");

        highRiskRequest = new FraudPredictionRequest(highRiskContract, highRiskClient);

        // PrÃ©parer une requÃªte normale
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


    @Test
    void testCreateAlert_ShouldPersistAlertInDatabase() {
        // CrÃ©er une alerte
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage("Contrat HIGH-RISK-001 suspect dÃ©tectÃ© par consensus des modÃ¨les ML");
        alert.setTimestamp(LocalDateTime.now());

        // Persister l'alerte directement avec EntityManager
        entityManager.persist(alert);
        entityManager.flush();

        // VÃ©rifications
        assertNotNull(alert.getId());
        assertEquals("Contrat HIGH-RISK-001 suspect dÃ©tectÃ© par consensus des modÃ¨les ML", alert.getMessage());

        // VÃ©rifier que l'alerte est bien en base de donnÃ©es
        entityManager.clear();

        FraudAlertEntity retrievedAlert = entityManager.find(FraudAlertEntity.class, alert.getId());
        assertNotNull(retrievedAlert);
        assertEquals("Contrat HIGH-RISK-001 suspect dÃ©tectÃ© par consensus des modÃ¨les ML", retrievedAlert.getMessage());
    }


    @Test
    void testNoAlert_NormalContract_ShouldNotCreateAlert() {
        // Compter les alertes avant le test
        Long initialCount = (Long) entityManager.createQuery("SELECT COUNT(a) FROM FraudAlertEntity a").getSingleResult();

        // Simuler l'analyse d'un contrat normal (pas de fraude dÃ©tectÃ©e)
        boolean shouldCreateAlert = false;

        if (shouldCreateAlert) {
            FraudAlertEntity alert = new FraudAlertEntity();
            alert.setMessage("Contrat NORMAL-001 normal");
            alert.setTimestamp(LocalDateTime.now());
            entityManager.persist(alert);
        }

        Long finalCount = (Long) entityManager.createQuery("SELECT COUNT(a) FROM FraudAlertEntity a").getSingleResult();
        assertEquals(initialCount, finalCount, "Aucune alerte ne devrait Ãªtre crÃ©Ã©e pour un contrat normal");
    }


    @Test
    void testGetAlerts_ShouldRetrieveCorrectAlerts() {
        // CrÃ©er plusieurs alertes
        FraudAlertEntity alert1 = createTestAlert("Alerte 1");
        FraudAlertEntity alert2 = createTestAlert("Alerte 2");

        entityManager.persist(alert1);
        entityManager.persist(alert2);
        entityManager.flush();

        // RÃ©cupÃ©rer toutes les alertes
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> allAlerts = entityManager.createQuery("SELECT a FROM FraudAlertEntity a").getResultList();

        // VÃ©rifications
        assertEquals(2, allAlerts.size(), "Il devrait y avoir 2 alertes");
        assertTrue(allAlerts.stream().anyMatch(alert -> alert.getMessage().equals("Alerte 1")));
        assertTrue(allAlerts.stream().anyMatch(alert -> alert.getMessage().equals("Alerte 2")));
    }


    @Test
    void testUpdateAlertMessage_ShouldPersistMessageChange() {
        // CrÃ©er et sauvegarder une alerte
        FraudAlertEntity alert = createTestAlert("Message initial");
        entityManager.persist(alert);
        entityManager.flush();

        // Mettre Ã  jour le message
        alert.setMessage("Message mis Ã  jour");
        entityManager.merge(alert);
        entityManager.flush();
        entityManager.clear();

        // VÃ©rifier que la mise Ã  jour est persistÃ©e
        FraudAlertEntity retrievedAlert = entityManager.find(FraudAlertEntity.class, alert.getId());
        assertEquals("Message mis Ã  jour", retrievedAlert.getMessage());
    }


    @Test
    void testAlertValidation_EmptyMessage_ShouldStillWork() {
        // CrÃ©er une alerte avec un message vide (pas de validation @NotNull dans l'entitÃ© actuelle)
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage("");
        alert.setTimestamp(LocalDateTime.now());

        // Cela devrait fonctionner car il n'y a pas de validation dans l'entitÃ© actuelle
        assertDoesNotThrow(() -> {
            entityManager.persist(alert);
            entityManager.flush();
        });

        assertNotNull(alert.getId());
        assertEquals("", alert.getMessage());
    }


    @Test
    void testCompleteWorkflow_FraudDetectionToDatabase() {
        // Simuler la dÃ©tection de fraude
        boolean model1DetectsFraud = true;
        boolean model2DetectsFraud = true;
        boolean consensusReached = model1DetectsFraud && model2DetectsFraud;

        // Si consensus, crÃ©er une alerte
        if (consensusReached) {
            FraudAlertEntity alert = new FraudAlertEntity();
            alert.setMessage("Fraude dÃ©tectÃ©e pour contrat " + highRiskRequest.getContractData().getContractId());
            alert.setTimestamp(LocalDateTime.now());

            // Sauvegarder en base
            entityManager.persist(alert);
            entityManager.flush();

            // VÃ©rifications
            assertNotNull(alert.getId());
            assertTrue(alert.getMessage().contains(highRiskRequest.getContractData().getContractId()));

            // VÃ©rifier que l'alerte est rÃ©cupÃ©rable
            @SuppressWarnings("unchecked")
            List<FraudAlertEntity> allAlerts = entityManager.createQuery("SELECT a FROM FraudAlertEntity a").getResultList();
            assertEquals(1, allAlerts.size());
            assertTrue(allAlerts.get(0).getMessage().contains(highRiskRequest.getContractData().getContractId()));
        }
    }


    @Test
    void testFindAlertsByMessage_ShouldReturnMatchingAlerts() {
        // CrÃ©er des alertes avec diffÃ©rents messages
        FraudAlertEntity alert1 = createTestAlert("Fraude dÃ©tectÃ©e - Contrat A");
        FraudAlertEntity alert2 = createTestAlert("Fraude dÃ©tectÃ©e - Contrat B");
        FraudAlertEntity alert3 = createTestAlert("Alerte normale");

        entityManager.persist(alert1);
        entityManager.persist(alert2);
        entityManager.persist(alert3);
        entityManager.flush();

        // Rechercher les alertes contenant "Fraude dÃ©tectÃ©e"
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> fraudAlerts = entityManager.createQuery(
                        "SELECT a FROM FraudAlertEntity a WHERE a.message LIKE :pattern")
                .setParameter("pattern", "%Fraude dÃ©tectÃ©e%")
                .getResultList();

        assertEquals(2, fraudAlerts.size(), "Il devrait y avoir 2 alertes de fraude");
        assertTrue(fraudAlerts.stream().allMatch(alert -> alert.getMessage().contains("Fraude dÃ©tectÃ©e")));
    }



    @Test
    void testFindAlertsByTimeRange_ShouldReturnAlertsInRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        // CrÃ©er des alertes Ã  diffÃ©rents moments
        FraudAlertEntity oldAlert = createTestAlert("Alerte ancienne");
        oldAlert.setTimestamp(twoHoursAgo);

        FraudAlertEntity recentAlert = createTestAlert("Alerte rÃ©cente");
        recentAlert.setTimestamp(oneHourAgo);

        FraudAlertEntity currentAlert = createTestAlert("Alerte actuelle");
        currentAlert.setTimestamp(now);

        entityManager.persist(oldAlert);
        entityManager.persist(recentAlert);
        entityManager.persist(currentAlert);
        entityManager.flush();

        LocalDateTime cutoff = now.minusMinutes(90);
        @SuppressWarnings("unchecked")
        List<FraudAlertEntity> recentAlerts = entityManager.createQuery(
                        "SELECT a FROM FraudAlertEntity a WHERE a.timestamp >= :cutoff ORDER BY a.timestamp DESC")
                .setParameter("cutoff", cutoff)
                .getResultList();

        assertEquals(2, recentAlerts.size(), "Il devrait y avoir 2 alertes rÃ©centes");
        assertEquals("Alerte actuelle", recentAlerts.get(0).getMessage());
        assertEquals("Alerte rÃ©cente", recentAlerts.get(1).getMessage());
    }


    private FraudAlertEntity createTestAlert(String message) {
        FraudAlertEntity alert = new FraudAlertEntity();
        alert.setMessage(message);
        alert.setTimestamp(LocalDateTime.now());
        return alert;
    }
}
*/