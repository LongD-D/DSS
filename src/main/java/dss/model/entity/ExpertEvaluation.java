package dss.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "expert_evaluation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"expert_id", "decision_id"})
        })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExpertEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User expert;

    @ManyToOne
    @JsonIgnore
    private Decision decision;

    @Min(0)
    @Max(10)
    private double score;

    @Min(0)
    @Max(1)
    private Double ca;

    @Min(0)
    @Max(1)
    private Double cs;

    @Min(0)
    @Max(1)
    private Double c;

    private String comment;
}
