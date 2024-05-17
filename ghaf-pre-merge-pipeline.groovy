#!/usr/bin/env groovy

// SPDX-FileCopyrightText: 2024 Technology Innovation Institute (TII)
//
// SPDX-License-Identifier: Apache-2.0

////////////////////////////////////////////////////////////////////////////////

def nix_build(flakeref) {
  try {
    sh "nix build -L ${flakeref}"
  } catch (Exception e) {
    // Mark the current step unstable
    unstable("Failed: ${flakeref}")
  }
}

def set_result() {
  if(currentBuild.result == "UNSTABLE") {
    // Fail the build if any step set the unstable status
    currentBuild.result = "FAILURE"
  }
}

////////////////////////////////////////////////////////////////////////////////

properties([
  githubProjectProperty(displayName: '', projectUrlStr: 'https://github.com/tiiuae/ghaf/'),
  // The following options are documented in:
  // https://www.jenkins.io/doc/pipeline/steps/params/pipelinetriggers/
  // Following config requires having github credentials configured in:
  // 'Manage Jenkins' > 'System' > 'Github' > 'GitHub Server' > 'Credentials'.
  pipelineTriggers([
    githubPullRequests(
      spec: '* * * * *',
      triggerMode: 'CRON',
      events: [Open(), commitChanged()],
      abortRunning: true,
      cancelQueued: true,
      preStatus: true,
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
          SCM = git(url: 'https://github.com/tiiuae/ghaf', branch: 'main')
        }
      }
    }
    stage('Checkout') {
      steps {
        sh 'set | grep GITHUB_PR'
        sh 'if [ -z "$GITHUB_PR_NUMBER" ]; then exit 1; fi'
        sh 'if [ -z "$GITHUB_PR_TARGET_BRANCH" ]; then exit 1; fi'
        sh 'rm -rf pr'
        sh 'git clone https://github.com/tiiuae/ghaf pr'
        dir('pr') {
          sh 'git fetch origin pull/$GITHUB_PR_NUMBER/head:pr_branch'
          sh 'git checkout -q pr_branch'
          sh 'git log -n1'
          // Rebase on top of the target branch
          sh 'git config user.email "foo@bar.com"; git config user.name "Foo Bar"'
          sh 'git rebase origin/$GITHUB_PR_TARGET_BRANCH'
          sh 'git log --oneline -n20'
        }
      }
    }
    stage('Set PR status pending') {
      steps {
        script {
          setGitHubPullRequestStatus(
            state: 'PENDING',
            context: 'ghaf-pre-merge-pipeline',
            message: 'Build started',
          )
        }
      }
    }
    stage("Build") {
      parallel {
        stage('x86_64') {
          steps {
            dir('pr') {
              nix_build('.#packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64')
              nix_build('.#packages.x86_64-linux.nvidia-jetson-orin-nx-debug-from-x86_64')
              nix_build('.#packages.x86_64-linux.lenovo-x1-carbon-gen11-debug')
              nix_build('.#packages.riscv64-linux.microchip-icicle-kit-debug')
              nix_build('.#packages.x86_64-linux.doc')
            }
          }
        }
        stage('aarch64') {
          steps {
            dir('pr') {
              nix_build('.#packages.aarch64-linux.nvidia-jetson-orin-agx-debug')
              nix_build('.#packages.aarch64-linux.nvidia-jetson-orin-nx-debug')
              nix_build('.#packages.aarch64-linux.doc')
            }
          }
        }
      }
    }
  }
  post {
    always {
      set_result()
    }
    success {
      script {
        echo 'Build passed, setting PR status SUCCESS'
        setGitHubPullRequestStatus(
          state: 'SUCCESS',
          context: 'ghaf-pre-merge-pipeline',
          message: 'Build passed',
        )
      }
    }
    unsuccessful {
      script {
        echo 'Build failed, setting PR status FAILURE'
        setGitHubPullRequestStatus(
          state: 'FAILURE',
          context: 'ghaf-pre-merge-pipeline',
          message: 'Build failed',
        )
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
