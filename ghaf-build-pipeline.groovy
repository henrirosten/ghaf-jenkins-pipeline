#!/usr/bin/env groovy

// SPDX-FileCopyrightText: 2024 Technology Innovation Institute (TII)
//
// SPDX-License-Identifier: Apache-2.0

////////////////////////////////////////////////////////////////////////////////

properties([
  // Poll every minute
  pipelineTriggers([pollSCM('* * * * *')]),
])

////////////////////////////////////////////////////////////////////////////////

pipeline {
  agent { label 'built-in' }
  options {
    timestamps ()
    buildDiscarder logRotator(
      artifactNumToKeepStr: '10',
      numToKeepStr: '10'
    )
  }
  stages {
    stage('Configure target repo') {
      steps {
        script {
          SCM = git(url: 'https://github.com/henrirosten/ghaf', branch: 'main')
        }
      }
    }
    stage('Build on x86_64') {
      steps {
        sh 'echo Build'
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
