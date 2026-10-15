package app.daos;

import java.util.List;

public interface GenericDAO<T, I> {
    // CREATE (persist)
    T create(T t);

    // READ (find)
    T getById(I id);
    List<T> getAll();
    boolean existsById(I id);

    // UPDATE (merge)
    T update(T t);

    // DELETE (remove)
    boolean delete(I id);
}
