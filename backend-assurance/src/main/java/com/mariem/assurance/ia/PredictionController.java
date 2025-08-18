package com.mariem.assurance.ia;

import com.mariem.assurance.assures.Assure;
import com.mariem.assurance.assures.AssureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/ia")
public class PredictionController {

    @Autowired
    private AssureRepository assureRepository;

    @PostMapping("/predict/{numContrat}")
    public ResponseEntity<String> predictFromAssure(@PathVariable Long numContrat) {
        Assure assure = assureRepository.findById(numContrat).orElse(null);

        if (assure == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Assuré non trouvé avec le numéro de contrat : " + numContrat);
        }

        DonneesInput input = IaUtils.fromAssure(assure);

        String url = "http://localhost:8000/predict"; // FastAPI

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, input, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la prédiction : " + e.getMessage());
        }
    }
}
