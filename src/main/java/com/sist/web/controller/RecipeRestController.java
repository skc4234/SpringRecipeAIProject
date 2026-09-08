package com.sist.web.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sist.web.service.RecipeService;

import lombok.RequiredArgsConstructor;
import java.util.*;

/*
 *   1. 전체 동작 과정
 *     <브라우저> : HTML / JavaScript(바닐라 JS)
 *        | => 재료 선택
 *    Thymeleaf
 *        | => Post '/recipe/recommend'
 *    RecipeController
 *        | @GetMapping("/recipe/recommand") => 화면 UI
 *        | @PostMapping("/recipe/recommand") => 데이터 전송
 *        | @ResponseBody: VO=>JSON/문자열로 변경해서 전송
 *        |  ==> +@Controller => @RestController
 *        | ingredients(재료) 전달
 *     RecipeService
 *        | 1) 재료 존재 여부 확인
 *        | 2) 검색문장 생성
 *        | 3) EmbeddingModel 생성
 *        | 4) String => float[] 변경(vector)
 *        | 5) PostgreSQL+pgVector => 유사 검색(Like)
 *        | 6) 레시피에서 content 추출
 *        | 7) 냉장고 <=> 레시피 재료 비교
 *        | 8) 재료 상태 결정(부족, 만족 등)
 *        | 9) 재료 충족률 계산
 *    주문 레시피 List(Limit 5)
 *    -----------------------
 *     => 보유 재료
 *        부족 재료
 *        재료 충족률
 *        레시피명
 *        조리방법
 *        요리 종류
 *        조리 과정
 *    ---------------------
 *        |
 *    Thymeleaf 화면
 *     => HTML => Controller => RecipeService
 *        ------------------- Spring AI
 *        => EmbeddingModel = PostgreSQL+pgVector
 *        => 유사 레시피 검색
 *        => 재료 확인
 *        -----------------> HTML에서 출력
 *        
 *    1. JavaScript => Pinia
 *    2. @ResponseBody => @RestController
 *    3. @Tool => Tool Calling(프롬프트)    
 */

@RestController
@RequestMapping("/recipe")
@RequiredArgsConstructor
public class RecipeRestController {
	private final RecipeService recipeVectorService;

    @GetMapping("/recommend")
    public String recommendPage(Model model) {

        /*
         * 처음에는 검색 결과가 없도록 설정
         */
        model.addAttribute(
                "recipes",
                Collections.emptyList()
        );

        return "recipe/recommend";
    }


    /**
     * ========================================================
     * 레시피 Vector 검색
     * ========================================================
     *
     * POST
     *
     * /recipe/recommend
     *
     * JSON
     *
     * {
     *   "ingredients": [
     *      "김치",
     *      "돼지고기",
     *      "두부"
     *   ]
     * }
     */
    @PostMapping("/recommend")
    @ResponseBody
    public Map<String, Object> recommend(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response =
                new HashMap<>();


        try {

            /*
             * JSON에서 ingredients 추출
             */
            Object ingredientObject =
                    request.get("ingredients");

            /*
             * 재료가 없는 경우
             */
            if (ingredientObject == null) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "재료를 선택해주세요."
                );

                response.put(
                        "recipes",
                        Collections.emptyList()
                );

                return response;
            }


            /*
             * JSON 배열 → List<String>
             */
            List<String> ingredients =
                    new ArrayList<>();

            if (ingredientObject instanceof List<?>) {

                List<?> list =
                        (List<?>) ingredientObject;

                for (Object value : list) {

                    if (value != null) {

                        String ingredient =
                                value.toString().trim();

                        if (!ingredient.isEmpty()) {

                            ingredients.add(
                                    ingredient
                            );
                        }
                    }
                }
            }


            /*
             * 선택 재료가 없는 경우
             */
            if (ingredients.isEmpty()) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "재료를 한 개 이상 선택해주세요."
                );

                response.put(
                        "recipes",
                        Collections.emptyList()
                );

                return response;
            }


            /*
             * =================================================
             * Vector 검색
             * =================================================
             */
            List<Map<String, Object>> recipes =
                    recipeVectorService.recommendRecipes(
                            ingredients
                    );


            /*
             * 정상 응답
             */
            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    recipes.isEmpty()
                            ? "추천 레시피가 없습니다."
                            : "레시피 추천이 완료되었습니다."
            );

            response.put(
                    "recipes",
                    recipes
            );


            /*
             * 사용자가 선택한 재료도 반환
             */
            response.put(
                    "selectedIngredients",
                    ingredients
            );


            return response;


        } catch (Exception e) {

            /*
             * 서버 로그
             */
            e.printStackTrace();


            /*
             * 오류 응답
             */
            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "레시피 검색 중 오류가 발생했습니다."
            );

            response.put(
                    "recipes",
                    Collections.emptyList()
            );

            return response;
        }
    }
}
