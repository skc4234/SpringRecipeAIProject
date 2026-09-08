const {defineStore}=Pinia
//const {ref}=Vue
/*
	- state: 공통으로 사용되는 변수
	 => 변경(상태)이 될때마다 HTML에 적용하는 변수
	 => 자바 static
	- getters: computed => 계산, 이미 지정된 값 => pagination, 수량 계산, 1000자리 콤마
	- actions: 실제 서버와 연결 => state 변수를 변경하는 역할
	
	==> React - Redux - TanstackQuery ==> NextJS
	  useState
	  
	Back-End
	 => Java / Oracle, MySQL(MariaDB), PostgreSQL
		SpringFramework / SpringBoot
		SpringSecurity / JWT
		WebSocket (STOMP / SockJS)
		Spring AI => EmbeddingModel / ChatClientModel
		          => RAG / MCP
		Kafka
    Front-End
	 => JQuery 4.0 => Ajax 유지보수
	    JSP / Thymeleaf
		Vue => Pinia
		React => TanStackQuery => Next.js
		TypeScript / NodeJS
		
	SE
	 => CI/CD => **Git Action (workflows)
	             **Docker / Docker-compose
				 AWS **EC2 / S3 / RDS
*/

const useRecipeStore=defineStore('recipe',{
	state:()=>{
		// 선택된 재료 배열
		const selectedIngredients=ref([])
		// 선택된 재료 카테고리
		const selectedCategory=ref("all")
		// AI 검색 로딩 여부
		const loading=ref(null)
		// 오류 메시지
		const errorMessage=ref('')
		// 화면에 표시할 재료 종류
		const ingredients=[
			{
			    name:"돼지고기",
			    icon:"🥩",
			    category:"육류"
			},

			{
			    name:"소고기",
			    icon:"🥩",
			    category:"육류"
			},

			{
			    name:"닭고기",
			    icon:"🍗",
			    category:"육류"
			},

			{
			    name:"김치",
			    icon:"🥬",
			    category:"채소"
			},

			{
			    name:"대파",
			    icon:"🌱",
			    category:"채소"
			},

			{
			    name:"양파",
			    icon:"🧅",
			    category:"채소"
			},

			{
			    name:"감자",
			    icon:"🥔",
			    category:"채소"
			},

			{
			    name:"당근",
			    icon:"🥕",
			    category:"채소"
			},

			{
			    name:"마늘",
			    icon:"🧄",
			    category:"채소"
			},

			{
			    name:"계란",
			    icon:"🥚",
			    category:"계란/유제품"
			},

			{
			    name:"두부",
			    icon:"🧊",
			    category:"계란/유제품"
			},

			{
			    name:"우유",
			    icon:"🥛",
			    category:"계란/유제품"
			},

			{
			    name:"치즈",
			    icon:"🧀",
			    category:"계란/유제품"
			},

			{
			    name:"쌀",
			    icon:"🍚",
			    category:"곡류"
			},

			{
			    name:"밀가루",
			    icon:"🌾",
			    category:"곡류"
			},

			{
			    name:"라면",
			    icon:"🍜",
			    category:"곡류"
			},

			{
			    name:"고춧가루",
			    icon:"🌶️",
			    category:"양념"
			},

			{
			    name:"고추장",
			    icon:"🫙",
			    category:"양념"
			},

			{
			    name:"된장",
			    icon:"🫙",
			    category:"양념"
			},

			{
			    name:"간장",
			    icon:"🍶",
			    category:"양념"
			},

			{
			    name:"소금",
			    icon:"🧂",
			    category:"양념"
			},

			{
			    name:"참치",
			    icon:"🐟",
			    category:"수산물"
			},

			{
			    name:"고등어",
			    icon:"🐟",
			    category:"수산물"
			},

			{
			    name:"새우",
			    icon:"🦐",
			    category:"수산물"
			}
		]
		const searchKeyword=ref('') // v-model
		const filteredIngredients=computed(()=>{
			const keyword=
				searchKeyword.value
			                 .trim()
							 .toLowerCase()
			return ingredients.value.filter(
				ingredient => {
					// 카테고리 조건 검사
					const categoryMatch=selectedCategory.value==='all'
					||
					ingredient.category===selectedCategory
					// 검색어 조건 검사
					const searchMatch=ingredient.name
					                            .toLowerCase()
												.includes(keyword)
					// 두 조건이 만족하는지
					return categoryMatch && searchMatch
				}
			)
		})
		function toggleIngredient(name) {
			// 현재 선택 여부 확인
			const index=selectedIngredients.value.indexOf(name)
			// 이미 선택된 상태인지
			if(index!==-1) {
				// 배열에서 삭제
				selectedIngredients.value.splice(index,1)
			}
			// 선택이 안된 상태
			else {
				selectedIngredients.value.push(name)
			}
		}
	},
	getters:{
		
	},
	actions:{
		
	}
})