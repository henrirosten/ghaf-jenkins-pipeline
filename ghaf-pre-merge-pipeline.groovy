#!/usr/bin/env groovy

// SPDX-FileCopyrightText: 2024 Technology Innovation Institute (TII)
//
// SPDX-License-Identifier: Apache-2.0

////////////////////////////////////////////////////////////////////////////////

properties([
  githubProjectProperty(displayName: '', projectUrlStr: 'https://github.com/henrirosten/ghaf/'),
  // The following options are documented in:
  // https://www.jenkins.io/doc/pipeline/steps/params/pipelinetriggers/
  // Following config requires having github credentials configured in:
  // 'Manage Jenkins' > 'System' > 'Github' > 'GitHub Server' > 'Credentials'.
  // Needs at least read/write access to commit statuses and pull requests.
  pipelineTriggers([
    githubPullRequests(
      spec: '* * * * *',
      triggerMode: 'CRON',
      events: [Open(), commitChanged()],
      abortRunning: false,
      cancelQueued: true,
      skipFirstRun: true,
    )
  ])
])

////////////////////////////////////////////////////////////////////////////////

pipeline {
  agent { label 'built-in' }
  options {
    timestamps ()
    buildDiscarder(logRotator(artifactNumToKeepStr: '10', numToKeepStr: '10'))
  }
  stages {
    stage('Configure target repo') {
      steps {
        script {
          SCM = git(url: 'https://github.com/henrirosten/ghaf', branch: 'main')
        }
      }
    }
    stage('Set PR status pending') {
      steps {
        script {
          setGitHubPullRequestStatus(
            state: 'PENDING',
            context: 'jenkins/pre-merge-pipeline',
            message: 'Build started',
          )
        }
      }
    }
    stage('Build on x86_64 (pre-merge)') {
      steps {
        sh 'echo "Would start x86_64 build here"'
      }
    }
  }
  post {
    success {
      script {
        echo 'Build passed, setting PR status SUCCESS'
        setGitHubPullRequestStatus(
          state: 'SUCCESS',
          context: 'jenkins/pre-merge-pipeline',
          message: 'Build passed',
        )
      }
    }
    failure {
      script {
        echo 'Build failed, setting PR status FAILURE'
        setGitHubPullRequestStatus(
          state: 'FAILURE',
          context: 'jenkins/pre-merge-pipeline',
          message: 'Build failed',
        )
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
