package ru.grnk.tradevisor.zcommon.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.News;

import static ru.grnk.tradevisor.dbmodel.Tables.NEWS;

@Repository
@RequiredArgsConstructor
public class NewsRepository {

    private final DSLContext dsl;

    public void saveNews(News news) {
        dsl.insertInto(NEWS, NEWS.SOURCE,
                        NEWS.HASH_ID,
                        NEWS.TITLE,
                        NEWS.DESCRIPTION,
                        NEWS.URL,
                        NEWS.PUBLISHED_AT
                )
                .values(news.getSource(),
                        news.getHashId(),
                        news.getTitle(),
                        news.getDescription(),
                        news.getUrl(),
                        news.getPublishedAt()
                )
                .onConflictDoNothing()
                .execute();
    }

}
