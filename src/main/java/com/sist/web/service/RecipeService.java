package com.sist.web.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import com.sist.web.mapper.PostgresMapper;

import lombok.RequiredArgsConstructor;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecipeService {
	private final PostgresMapper recipeMapper;
	private final EmbeddingModel embeddingModel;
	
    public List<Map<String, Object>> recommendRecipes(
            List<String> ingredients) {

        /*
         * 재료가 없으면 검색하지 않는다.
         */
        if (ingredients == null || ingredients.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * ----------------------------------------------------
         * 1. 검색 문장 생성
         * ----------------------------------------------------
         *
         * 예:
         *
         * 김치, 돼지고기, 두부, 대파, 양파
         *
         * →
         *
         * "김치 돼지고기 두부 대파 양파를 이용할 수 있는 레시피"
         */
        String queryText = createQueryText(ingredients);

        /*
         * ----------------------------------------------------
         * 2. 검색 문장을 Embedding으로 변환
         * ----------------------------------------------------
         */
        float[] vector = embeddingModel.embed(queryText);

        /*
         * ----------------------------------------------------
         * 3. float[] → PostgreSQL vector 문자열
         * ----------------------------------------------------
         *
         * pgVector에서는 다음 형태를 사용한다.
         *
         * [0.123,-0.234,0.456,...]
         */
        String vectorString = convertVectorToString(vector);

        /*
         * ----------------------------------------------------
         * 4. PostgreSQL pgVector 유사 검색
         * ----------------------------------------------------
         */
        List<Map<String, Object>> recipes =
                recipeMapper.findSimilarRecipes(
                        vectorString,
                        5
                );

        /*
         * ----------------------------------------------------
         * 5. 각 레시피에 대한 재료 충족률 계산
         * ----------------------------------------------------
         */
        for (Map<String, Object> recipe : recipes) {

            /*
             * PostgreSQL 컬럼명
             *
             * recipe_id
             * content
             * similarity
             */
            Object contentObject = recipe.get("content");

            String content = "";

            if (contentObject != null) {
                content = contentObject.toString();
            }

            /*
             * 레시피 content에서 재료 부분을 추출
             */
            List<String> recipeIngredients =
                    extractIngredients(content);

            /*
             * 사용자가 가지고 있는 재료와
             * 레시피 재료 비교
             */
            Map<String, Object> ingredientStatus =
                    calculateIngredientStatus(
                            ingredients,
                            recipeIngredients
                    );

            recipe.put(
                    "haveIngredients",
                    ingredientStatus.get("haveIngredients")
            );

            recipe.put(
                    "missingIngredients",
                    ingredientStatus.get("missingIngredients")
            );

            recipe.put(
                    "ingredientRate",
                    ingredientStatus.get("ingredientRate")
            );

            /*
             * 레시피 이름 추출
             */
            recipe.put(
                    "recipeName",
                    extractRecipeName(content)
            );

            /*
             * 조리방법 추출
             */
            recipe.put(
                    "cookingMethod",
                    extractValue(content, "조리방법")
            );

            /*
             * 요리종류 추출
             */
            recipe.put(
                    "foodType",
                    extractValue(content, "요리종류")
            );

            /*
             * 요리팁 추출
             */
            recipe.put(
                    "tip",
                    extractValue(content, "요리팁")
            );

            /*
             * 조리정보
             *
             * 데이터에 이미지 URL이 저장되어 있다면
             * HTML에서 사용할 수 있다.
             */
            recipe.put(
                    "recipeImage",
                    extractValue(content, "조리정보")
            );

            /*
             * 조리 과정 추출
             */
            recipe.put(
                    "steps",
                    extractCookingSteps(content)
            );
        }

        return recipes;
    }


    /**
     * ========================================================
     * 검색 문장 생성
     * ========================================================
     */
    private String createQueryText(List<String> ingredients) {

        StringBuilder sb = new StringBuilder();

        sb.append("냉장고에 있는 재료를 이용할 수 있는 레시피. ");
        sb.append("사용 가능한 재료: ");

        for (String ingredient : ingredients) {

            if (ingredient == null) {
                continue;
            }

            String value = ingredient.trim();

            if (!value.isEmpty()) {
                sb.append(value).append(" ");
            }
        }

        return sb.toString().trim();
    }


    /**
     * ========================================================
     * float[] → PostgreSQL vector 문자열
     * ========================================================
     *
     * 예:
     *
     * [0.123,-0.234,0.345]
     */
    private String convertVectorToString(float[] vector) {

        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException(
                    "Embedding vector가 비어 있습니다."
            );
        }

        StringBuilder sb = new StringBuilder();

        sb.append("[");

        for (int i = 0; i < vector.length; i++) {

            if (i > 0) {
                sb.append(",");
            }

            sb.append(vector[i]);
        }

        sb.append("]");

        return sb.toString();
    }


    /**
     * ========================================================
     * 레시피 이름 추출
     * ========================================================
     *
     * content 예:
     *
     * 레시피명: 버섯 두유 소스 볶음
     *
     * → 버섯 두유 소스 볶음
     */
    private String extractRecipeName(String content) {

        String value = extractValue(content, "레시피명");

        if (value.isEmpty()) {
            return "추천 레시피";
        }

        return value;
    }


    /**
     * ========================================================
     * content에서 특정 항목 추출
     * ========================================================
     *
     * 예:
     *
     * 조리방법: 볶기
     *
     * → 볶기
     */
    private String extractValue(
            String content,
            String key) {

        if (content == null || content.isEmpty()) {
            return "";
        }

        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {

            String trim = line.trim();

            if (trim.startsWith(key + ":")) {

                return trim
                        .substring((key + ":").length())
                        .trim();
            }
        }

        return "";
    }


    /**
     * ========================================================
     * 레시피 재료 추출
     * ========================================================
     *
     * 현재 recipe_vector의 content가
     *
     * 주재료:
     * 새송이버섯 100g
     * 두유 20g
     * 생크림 10g
     *
     * 형태이므로 "주재료" 이후 내용을 분석한다.
     *
     * 정확한 DB 포맷이 더 세분화되어 있다면
     * 이 부분만 수정하면 된다.
     */
    private List<String> extractIngredients(String content) {

        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> ingredients = new HashSet<>();

        /*
         * 재료 키워드
         */
        String[] knownIngredients = {

            "김치",
            "돼지고기",
            "소고기",
            "닭고기",
            "두부",
            "대파",
            "양파",
            "감자",
            "당근",
            "마늘",
            "계란",
            "우유",
            "치즈",
            "쌀",
            "밀가루",
            "라면",
            "고춧가루",
            "고추장",
            "된장",
            "간장",
            "소금",
            "참치",
            "고등어",
            "새우",
            "버섯",
            "새송이버섯",
            "청양고추",
            "후추",
            "올리브유",
            "두유",
            "생크림",
            "새싹채소"
        };

        /*
         * content 전체에서 알려진 재료 검색
         *
         * 프로젝트에서 사용하는 실제 재료 목록을
         * 이 배열에 추가하면 된다.
         */
        for (String ingredient : knownIngredients) {

            if (content.contains(ingredient)) {
                ingredients.add(ingredient);
            }
        }

        return new ArrayList<>(ingredients);
    }


    /**
     * ========================================================
     * 재료 충족률 계산
     * ========================================================
     *
     * 예:
     *
     * 필요한 재료
     * 김치
     * 돼지고기
     * 두부
     * 대파
     * 양파
     * 고춧가루
     *
     * 가지고 있는 재료
     * 김치
     * 돼지고기
     * 두부
     * 대파
     * 양파
     *
     * 충족률
     *
     * 5 / 6 × 100
     * = 83.3%
     */
    private Map<String, Object> calculateIngredientStatus(
            List<String> userIngredients,
            List<String> recipeIngredients) {

        Map<String, Object> result = new HashMap<>();

        List<String> have =
                new ArrayList<>();

        List<String> missing =
                new ArrayList<>();

        /*
         * 레시피 재료가 없으면
         * 계산할 수 없으므로 0%
         */
        if (recipeIngredients == null
                || recipeIngredients.isEmpty()) {

            result.put(
                    "haveIngredients",
                    have
            );

            result.put(
                    "missingIngredients",
                    missing
            );

            result.put(
                    "ingredientRate",
                    0.0
            );

            return result;
        }

        /*
         * 사용자 재료를 소문자 Set으로 만든다.
         */
        Set<String> userSet =
                new HashSet<>();

        for (String ingredient : userIngredients) {

            if (ingredient != null) {

                userSet.add(
                        normalizeIngredient(
                                ingredient
                        )
                );
            }
        }

        /*
         * 레시피 재료 비교
         */
        for (String recipeIngredient :
                recipeIngredients) {

            String normalized =
                    normalizeIngredient(
                            recipeIngredient
                    );

            boolean exists = false;

            for (String userIngredient :
                    userSet) {

                /*
                 * 예:
                 *
                 * 새송이버섯
                 * 버섯
                 *
                 * 일부 포함 관계도 인정
                 */
                if (normalized.contains(userIngredient)
                        || userIngredient.contains(normalized)) {

                    exists = true;
                    break;
                }
            }

            if (exists) {

                have.add(recipeIngredient);

            } else {

                missing.add(recipeIngredient);
            }
        }

        /*
         * 충족률
         */
        double rate =
                ((double) have.size()
                        / recipeIngredients.size())
                        * 100.0;

        /*
         * 소수점 1자리
         */
        rate =
                Math.round(rate * 10.0)
                        / 10.0;

        result.put(
                "haveIngredients",
                have
        );

        result.put(
                "missingIngredients",
                missing
        );

        result.put(
                "ingredientRate",
                rate
        );

        return result;
    }


    /**
     * ========================================================
     * 재료 문자열 정규화
     * ========================================================
     */
    private String normalizeIngredient(
            String ingredient) {

        if (ingredient == null) {
            return "";
        }

        return ingredient
                .replaceAll(
                        "[0-9]+(\\.\\d+)?",
                        ""
                )
                .replaceAll(
                        "(g|kg|ml|L|개|모|대|큰술|작은술|컵|쪽|약간)",
                        ""
                )
                .replaceAll(
                        "\\([^)]*\\)",
                        ""
                )
                .trim()
                .toLowerCase();
    }


    /**
     * ========================================================
     * 조리 과정 추출
     * ========================================================
     *
     * DB content의 문장 중
     * 실제 조리 설명을 화면에 출력하기 위한 메소드
     *
     * 현재 데이터가 구조화되어 있지 않은 경우
     * content의 문장을 분리한다.
     */
    private List<String> extractCookingSteps(
            String content) {

        List<String> steps =
                new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return steps;
        }

        /*
         * "조리정보" 이후는 이미지 URL이므로 제외한다.
         */
        String cookingPart = content;

        int imageIndex =
                cookingPart.indexOf("조리정보:");

        if (imageIndex >= 0) {

            cookingPart =
                    cookingPart.substring(
                            0,
                            imageIndex
                    );
        }

        /*
         * 문장을 분리한다.
         */
        String[] sentences =
                cookingPart.split(
                        "(?<=[.!?])\\s+"
                );

        for (String sentence :
                sentences) {

            String value =
                    sentence.trim();

            /*
             * 너무 짧은 문장 제외
             */
            if (value.length() >= 5
                    && !value.startsWith("레시피명:")
                    && !value.startsWith("영양정보:")
                    && !value.startsWith("탄수화물:")
                    && !value.startsWith("단백질:")
                    && !value.startsWith("지방:")
                    && !value.startsWith("나트륨:")
                    && !value.startsWith("해시태그:")
                    && !value.startsWith("주재료:")) {

                steps.add(value);
            }

            /*
             * 화면이 너무 길어지는 것을 방지
             */
            if (steps.size() >= 6) {
                break;
            }
        }

        return steps;
    }
}
