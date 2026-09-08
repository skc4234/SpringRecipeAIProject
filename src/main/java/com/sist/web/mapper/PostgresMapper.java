package com.sist.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.*;
import com.sist.web.vo.*;

@Mapper
@Repository
public interface PostgresMapper {
	public List<Map<String, Object>> findSimilarRecipes( 
			@Param("embedding") String embedding, 
			@Param("limit") int limit );
}
