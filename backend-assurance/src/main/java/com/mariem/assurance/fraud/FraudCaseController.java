package com.mariem.assurance.fraud;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/fraud/cases")
@RequiredArgsConstructor
@CrossOrigin // enlève si tu gères CORS ailleurs
public class FraudCaseController {
    private final JdbcTemplate jdbc;
    private FraudCaseService fraudCaseService;

    @GetMapping
    public List<Map<String,Object>> list(
            @RequestParam(required=false) String entity,   // ASSURE | SINISTRE | null
            @RequestParam(defaultValue="50") int minScore,
            @RequestParam(defaultValue="OPEN") String status // OPEN | REVIEWED | ALL
    ){
        String base = """
      SELECT id, entity_type, entity_id, score, risk_level, reason, detected_at, status
      FROM fraud_cases
      WHERE score >= ?
    """;
        List<Object> args = new ArrayList<>();
        args.add(minScore);

        if (entity != null && !entity.isBlank()) { base += " AND entity_type = ? "; args.add(entity); }
        if (!"ALL".equalsIgnoreCase(status))       { base += " AND status = ? ";     args.add(status); }

        base += " ORDER BY detected_at DESC";
        return jdbc.queryForList(base, args.toArray());
    }

    // Optionnel : marquer comme “REVIEWED”
    @PatchMapping("/{id}")
    public void review(@PathVariable long id){
        jdbc.update("UPDATE fraud_cases SET status='REVIEWED' WHERE id=?", id);
    }


    @PostMapping("/record")
    public void record(@RequestBody RecordReq req) {
        fraudCaseService.recordIfFraud(req.entityType(), req.entityId(), req.score(), req.reason());
    }

    // petite DTO interne
    public record RecordReq(String entityType, String entityId, int score, String reason) {}
    
}
