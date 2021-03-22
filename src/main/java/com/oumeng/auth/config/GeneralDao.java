package com.oumeng.auth.config;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GeneralDao {

    @Insert({"<script>",
            "insert into ${table}",
            "<foreach collection='data' index='key' item='value' open='(' close=')' separator=','>${key}</foreach>",
            "values",
            "<foreach collection='data' index='key' item='value' open='(' close=')' separator=','>#{value}</foreach>",
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "data.id")
    void insert(String table, Map<String, Object> data);

    @Select({"select ${column} from ${table} where ${condition}"})
    List<Map<String, Object>> query(String column, String table, String condition);
}
