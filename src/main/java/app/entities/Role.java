package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Role {
    @Id
    @Column(length = 20)
    private String roleName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return roleName != null && roleName.equals(other.getRoleName());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
