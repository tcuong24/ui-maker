package com.designmd.designapi.crawl;

import com.designmd.designapi.crawl.response.CrawledPageResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CrawledPageMapper {

    CrawledPageResponse toResponse(
            CrawledPage page
    );

    List<CrawledPageResponse> toResponse(
            List<CrawledPage> pages
    );
}