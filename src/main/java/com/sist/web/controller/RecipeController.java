package com.sist.web.controller;

import org.springframework.stereotype.Controller;
/*
 *   전체 구조
 *     HTML(재료 선택) => 사용자
 *          |
 *    RecipeController => Router
 *          |
 *       재료 선택
 *          |
 *     RecipeRestController
 *          |
 *    ---------------
 *     RecipeService
 *     Mapper
 *    ---------------
 *          |
 *      EmbeddingModel
 *      String => float[]
 *          |
 *    ----------------
 *     vector => [0.1,-0.2,...]
 *    ----------------
 *          |
 *       Mapper => findSimilarRecipe() => 유사도
 *          |
 *    -----------------------
 *     PostgreSQL + pgVector
 *     embedding => 검색
 *     Cosine Distance => 유사도 검색(가장 가까운 거리 측정 => 실수(String=>float))
 *    ------------------------
 *          | => vectorDB
 *       distance => 작은 순으로 출력: 맛집 / 쇼핑몰
 *          |
 *        order by => limit 5
 *          |
 *   --------------------
 *       재료 비교
 *          |
 *    AVA(보유) / SHO(부족) / SUB(대체)
 *   ------------------------
 *          |
 *   ------------------------
 *     ingredientRate
 *     ingredients
 *     misssingingredient
 *  ---------------------------
 *          |
 *       Thymeleaf
 *        => 충족률 : 75%
 *        => 재료 : ... 
 *        
 *    
 *    
 */
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class RecipeController {
	@GetMapping("/recipe/recommand")
	public String recipe_recommand() {
		return "recipe/recommand1";
	}
}
