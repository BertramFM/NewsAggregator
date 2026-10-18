package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiProvider apiProvider;

    @Column(nullable = false)
    private String url;

    // ======== Relations ========
    @Builder.Default
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Article> articles = new ArrayList<>();
    // ===========================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Source other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
