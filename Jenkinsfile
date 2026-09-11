pipeline {
	/*
	   규모에 따른 CI/CD
		- Git Action
		- Jenkins
		- 자체 처리
		- docker, docker-compose
	
		전체 동작: Jenkins => 관리자
		Git push
		   | ---- workflows(Git)
	       | ---- WebHook (트리거)
	    Jenkins
	       | ---- Permission 방지
	       | ---- chmod +x gradlew : 실행 권한
	  Gradle Build
	       | ---- ./gradlew clean build -x test => test 제외하고 .jar 묶기
	  Docker Build
	       | ---- docker build -t image명 . => docker image 생성
	  Docker Hub Push
	       | ---- docker push image명
	  Docker compose down
	       |
	  Docker compose Pull
	       |
	  Docker compose up -d
	*/
	agent any
	environment {
		LOCAL_APP_DIR="~/app"
		JAR_NAME="SpringRecipeAIProject-0.0.1-SNAPSHOT.jar"
		DOCKER_IMAGE = "skc4234/ai-app:latest"
		// AWS EC2
		SERVER_USER="ubuntu"
		SERVER_IP="43.203.176.171"
		SERVER_APP_DIR="/home/ubuntu/app"
	}
	// 우분투 (AWS) 명령어 수행
	stages {
		// 1. Git Checkout: Repositoy 존재 여부 확인
		stage("Repository Checkout"){
			steps {
				echo 'Git Checkout'
				checkout scm
			}
		}
		
		// 2. Java JDK 버전 확인
		stage("JDK 21 확인"){
			steps {
				sh '''
					java -version
					./gradlew --version
				'''
			}
		}
		
		// 3. gradlew 실행 권한
		stage("Gradlew Permission"){
			steps {
				sh '''
					chmod +x gradlew
				'''
			}
		}
		
		// 4. gradlew build => 배포 파일 만들기
		stage("Gradlew Build"){
			steps {
				sh '''
					./gradlew clean build -x test
				'''
			}
		}
		
		// 5. Docker Image => 시간 측정
		stage("Docker Build"){
			steps {
				sh '''
					docker build -t ${DOCKER_IMAGE} .
				'''
			}
		}
		
		// 6. Docker Hub Login
		stage("DockerHub Login"){
			steps {
				withCredentials([
					usernamePassword(
						credentialsId: 'dockerhub_info',
						usernameVariable: 'DH_USER',
						passwordVariable: 'DH_PASS'
					)
				]){
					sh '''
						echo "$DH_PASS" docker login -u "$DH_USER" --password-stdin 
					'''
				}
			}
		}
		
		// 7. DockerHub Push
		stage("DockerHub Push"){
			steps {
				sh '''
					docker push ${DOCKER_IMAGE}
				'''
			}
		}
		
		// 8. SSH Key 설정 (SERVER_SSH_KEY)
		stage("SSH Key Setting"){
			steps {
				withCredentials([
					sshUserPrivateKey(
						credentialsId: 'SERVER_SSH_KEY',
						keyFileVariable: 'SSH_KEY',
						usernameVariable: 'SSH_USER'
					)
				]){
					sh '''
						mkdir -p ~/.ssh
						cp "$SSH_KEY" ~/.ssh/id_ed25519
						chmod 600 ~/.ssh/id_ed25519
					'''
				}
			}
		}		
		
		// 9. AWS 접근
		stage("Known Hosts"){
			steps {
				sh '''
					mkdir -p ~/.ssh
					ssh-keyscan -H 43.203.176.171 >> ~/.ssh/known_hosts
					
					chmod 644 ~/.ssh/known_hosts
				'''
			}
		}		
		
		// 10. .env 생성
		stage("Create .env"){
			steps {
				withCredentials([
					string(
						credentialsId: 'post-url',
						variable: 'POST_URL'
					),
					string(
						credentialsId: 'gen-key',
						variable: 'GEN_KEY'
					),
					sshUserPrivateKey(
						credentialsId: 'SERVER_SSH_KEY',
						keyFileVariable: 'SSH_KEY',
						usernameVariable: 'SSH_USER'
					)
				]){
					sh '''
						ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ubuntu@43.203.176.171<<EOF
				        mkdir -p /home/ubuntu/app
				        cd /home/ubuntu/app
				        rm -f .env
				        echo "SPRING_PROFILES_ACTIVE=prod" > .env
			            echo "POST_URL=${POST_URL}" >> .env
			            echo "GEN_KEY=${GEN_KEY}" >> .env
			            
			            chmod 600 .env
			            
			            EOF
					'''
				}
			}
		}
		
		// 11. docker-compose.yml 이동
		stage("Copy Docker-Compose"){
			steps {
				withCredentials([
					sshUserPrivateKey(
						credentialsId: 'SERVER_SSH_KEY',
						keyFileVariable: 'SSH_KEY',
						usernameVariable: 'SSH_USER'
					)
				]){
					sh '''
						ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ubuntu@43.203.176.171 "mkdir -p /home/ubuntu/app"
				        scp -i "$SSH_KEY" -o StrictHostKeyChecking=no ubuntu@43.203.176.171 docker-compose.yml ubuntu@43.203.176.171:/home/ubuntu/app/docker-compose.yml
						
					'''
				}
			}
		}		
		
		// 12. 배포
		stage("Deploy"){
			steps {
				withCredentials([
					sshUserPrivateKey(
						credentialsId: 'SERVER_SSH_KEY',
						keyFileVariable: 'SSH_KEY',
						usernameVariable: 'SSH_USER'
					)
				]){
					sh '''
						ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ubuntu@43.203.176.171<<EOF
						cd /home/ubuntu/app
						docker-compose down
						docker-compose pull
						docker-compose up -d
						
						EOF
					'''
				}
			}
		}
	}
}
// pipeline 종료
post {
	success {
		echo '======================'
		echo 'Docker Compose 배포 성공'
		echo '======================'
	}
	failure {
		echo '======================'
		echo 'Docker Compose 배포 실패'
		echo '======================'
		sh '''
			docker compose ps || true
		'''
	}
}