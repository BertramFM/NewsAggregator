package app.config;

import app.entities.*;
import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {
        configuration.addAnnotatedClass(Article.class);
        configuration.addAnnotatedClass(DigestLog.class);
        configuration.addAnnotatedClass(Topic.class);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Subscription.class);
        configuration.addAnnotatedClass(Source.class);
    }
}