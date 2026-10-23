package app.controllers;

import app.daos.ArticleDAO;
import app.dtos.ArticleDTO;
import app.dtos.ArticleWithTextDTO;
import app.entities.Article;
import app.exceptions.ApiException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class ArticleController {
    private final ArticleDAO articleDAO;

    public ArticleController(ArticleDAO articleDAO) {
        this.articleDAO = articleDAO;
    }

    public void getArticles(Context ctx) {
        List<Article> articles = articleDAO.getAll();
        List<ArticleDTO> dto = articles.stream()
                .map(ArticleDTO::new)
                .toList();
        ctx.json(dto);
    }

    public void delete(Context ctx) {
        Long id = parseId(ctx);
        articleDAO.delete(id);
        ctx.status(HttpStatus.OK);
        ctx.json(Map.of(
                "message", "Article deleted",
                "id", id
        ));
    }

    public void getById(Context ctx) {
        Long id = parseId(ctx);
        Article article = articleDAO.getById(id);

        if (article == null) {
            throw new ApiException(404, "Article not found with id: " + id);
        }

        ctx.status(HttpStatus.OK);
        ctx.json(new ArticleWithTextDTO(article));
    }

    private Long parseId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid ID format: " + ctx.pathParam("id"));
        }
    }
}
