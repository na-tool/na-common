package com.na.common.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Schema(description = "分页返回结果")
public class BasePagesDomain<T> {
    @Schema(description = "当前页码", example = "1")
    private long currentPage;
    @Schema(description = "总条数", example = "100")
    private long total;
    @Schema(description = "总页数", example = "10")
    private long totalPage;
    @Schema(description = "每页记录条数", example = "10")
    private long pageSize;
    @Schema(description = "返回数据对象集")
    private List<T> totalList;

    public BasePagesDomain() {
        this.currentPage = 1;
        this.pageSize = 10;
        this.total = 0;
        this.totalPage = 0;
        totalList = new ArrayList<>();
    }

    public BasePagesDomain(IPage<T> iPage) {
        if (iPage != null) {
            this.currentPage = iPage.getCurrent();
            this.pageSize = iPage.getSize();
            this.total = iPage.getTotal();
            this.totalPage = iPage.getPages();
            this.totalList = iPage.getRecords();
        }
    }

    public BasePagesDomain(long currentPage,
                           long pageSize,
                           long totalPage,
                           long total) {
        this.currentPage = currentPage;
        this.total = total;
        this.totalPage = totalPage;
        this.pageSize = pageSize;
    }

    public BasePagesDomain(long currentPage,
                           long pageSize,
                           long totalPage,
                           long total,
                           List<T> totalList) {
        this.currentPage = currentPage;
        this.total = total;
        this.totalPage = totalPage;
        this.pageSize = pageSize;
        this.totalList = totalList != null ? totalList : new ArrayList<>();
    }

    public static <T,B> BasePagesDomain<B> pages(IPage<T> page, List<B> data){
        BasePagesDomainBuilder<B> builder = BasePagesDomain.<B>builder().totalList(data);
        if(page != null){
            builder.currentPage(page.getCurrent())
                    .pageSize(page.getTotal())
                    .totalPage(page.getPages())
                    .total(page.getSize());
        }
        return builder.build();
    }

    public static <B> BasePagesDomain<B> pages(Page<B> page) {
        BasePagesDomainBuilder<B> builder = BasePagesDomain.<B>builder()
                .totalList(page.getRecords());
        if (page != null) {
            builder.currentPage(page.getCurrent())
                    .pageSize(page.getSize())
                    .totalPage(page.getPages())
                    .total(page.getTotal());
        }
        return builder.build();
    }

    public static <T> Page<T> pages(IPage<T> iPage) {
        Page<T> resultPage = new Page<>();
        if (iPage != null) {
            resultPage.setCurrent(iPage.getCurrent());
            resultPage.setSize(iPage.getSize());
            resultPage.setTotal(iPage.getTotal());
            resultPage.setRecords(iPage.getRecords());
        }
        return resultPage;
    }

    public static <T> Page<T> pages(Page<T> p, IPage<T> iPage) {
        Page<T> resultPage = new Page<>();
        if (iPage != null) {
            resultPage.setCurrent(p.getCurrent());
            resultPage.setSize(p.getSize());
            resultPage.setTotal(iPage.getTotal());
            resultPage.setRecords(iPage.getRecords());
        }
        return resultPage;
    }

    public static <T,B> IPage<B> iPages(IPage<T> page, List<B> data){
        IPage<B> modelIPage = new Page<>();
        modelIPage.setRecords(data);
        if(page != null){
            modelIPage.setPages(page.getPages());
            modelIPage.setCurrent(page.getCurrent());
            modelIPage.setSize(page.getSize());
            modelIPage.setTotal(page.getTotal());
        }
        return modelIPage;
    }

}

