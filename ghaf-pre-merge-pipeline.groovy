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
      userRestriction: [users: '', orgs: 'tiiuae'],
    )
  ])
])

////////////////////////////////////////////////////////////////////////////////

pipeline {
  agent { label 'built-in' }
  options {
    timestamps ()
    buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: '100'))
  }
  stages {
    stage('Configure target repo') {
      steps {
        script {
          SCM = git(url: 'https://github.com/henrirosten/ghaf', branch: 'main')
        }
      }
    }
    stage('Init') {
      steps {
        sh 'set | grep GITHUB'
        sh 'if [ -z "$GITHUB_PR_HEAD_SHA" ]; then exit 1; fi'
        sh 'if [ -z "$GITHUB_PR_URL" ]; then exit 1; fi'
        sh 'if [ -z "$GITHUB_PR_NUMBER" ]; then exit 1; fi'
      }
    }
    stage('Checkout') {
      steps {
        sh 'rm -rf pr'
        sh 'git clone $(echo $GITHUB_PR_URL | sed "s|/pull/$GITHUB_PR_NUMBER||g") pr'
        dir('pr') {
          sh 'git checkout -q $GITHUB_PR_HEAD_SHA'
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
    stage('Build on x86_64') {
      steps {
        dir('pr') {
          sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64'
          sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-nx-debug-from-x86_64'
          sh 'nix build -L .#packages.x86_64-linux.lenovo-x1-carbon-gen11-debug'
          sh 'nix build -L .#packages.riscv64-linux.microchip-icicle-kit-debug'
          sh 'nix build -L .#packages.doc'
        }
      }
    }
    stage('Build on aarch64') {
      steps {
        dir('pr') {
          sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-agx-debug'
          sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-nx-debug'
          sh 'nix build -L .#packages.aarch64-linux.imx8qm-mek-debug'
          sh 'nix build -L .#packages.aarch64-linux.doc'
        }
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
