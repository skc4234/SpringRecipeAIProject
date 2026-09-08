pipeline {
	agent any
	environment {
		APP_DIR="~/app"
		JAR_NAME="SpringRecipeAIProject-0.0.1-SNAPSHOT.jar"
	}
	stages {
		/*
			git push => commit (main)
			   |
			web hook / poll
			   |
			 Jenkins (local) = EC2
			   |
			 build
			   |
			 docker build
			 docker push
			   |
			 docker pull
			 docker run
		*/
		/*
			Repository: 소스파일 => Git URL
		*/
		stage('Check Out'){ // git actions의 name:
			steps { // git actions의 run:
				echo 'Git Checkout'
				checkout scm
			}
		}
		
		// 환경설정 파일 추가
		stage('Create .env'){
			steps{
				withCredentials([
					string(
						credentialsId: 'post-url',
						variable: 'POST_URL'
					),
					string(
						credentialsId: 'gen-key',
						variable: 'GEN_KEY'			
					)
				]) {
					sh '''
						SPRING_PROFILES_ACTIVE=prod > .env
						POST_URL=${POST_URL} >> .env
						GEN_KEY=${GEN_KEY} >> .env
						chmod 600 .env
					'''
				}
			}
		}
		
		// gradlew build 전 permission 처리
		stage('Gradlew permission'){
			steps{
				sh '''
					chmod +x gradlew 	
				'''
			}
		}
		
		// gradlew build
		stage('Gradlew build'){
			steps{
				sh '''
					./gradlew clean build -x test
				'''
			}
		}
		
		// docker container 생성
		stage('Docker Build'){
			steps{
				sh '''
					docker build -t skc4234/ai-app:latest . 	
				'''
			}
		}
		
		// dockerhub login
		stage('Dockerhub Login'){
			steps{
				withCredentials([usernamePassword(
					credentialsId:'dockerhub_info',
					usernameVariable:'DH_USER',
					passwordVariable:'DH_PASS'
				)]) {
					sh '''
						echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
					'''
				}
			}
		}
		
		// docker hub에 push
		stage('Docker Push'){
			steps{
				sh '''
					docker push skc4234/ai-app:latest
				'''
			}
		}
		
		// Container Stop
		stage('Container Stop'){
			steps{
				sh '''
					docker stop ai-app || true
				'''
			}
		}
		
		// Container Remove
		stage('Container Remove'){
			steps{
				sh '''
					docker rm ai-app || true
				'''
			}
		}
		
		// Dockerhub Pull
		stage('Dockerhub Pull'){
			steps{
				sh '''
					docker pull skc4234/ai-app:latest
				'''
			}
		}
		
		// Container Remove
		stage('Docker Run'){
			steps{
				sh '''
					docker run -d --name ai-app --env-file .env -p 9090:9090 skc4234/ai-app:latest || true
				'''
			}
		}
	}
}