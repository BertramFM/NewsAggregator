package app.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String topicName;


    // ======== Relations ========
    @Builder.Default
    @ManyToMany(mappedBy = "topics")
    private List<Article> articles = new ArrayList<>();
    // ===========================


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Topic other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
