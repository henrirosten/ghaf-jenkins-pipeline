#!/usr/bin/env groovy

// SPDX-FileCopyrightText: 2024 Technology Innovation Institute (TII)
//
// SPDX-License-Identifier: Apache-2.0

////////////////////////////////////////////////////////////////////////////////

// https://www.jenkins.io/doc/pipeline/steps/params/pipelinetriggers/
properties([
  githubProjectProperty(displayName: '', projectUrlStr: 'https://github.com/henrirosten/ghaf/'),
  pipelineTriggers([
    pollSCM('* * * * *'),
    githubPullRequests(
      spec: '* * * * *',
      triggerMode: 'CRON',
      events: [Open(), commitChanged()],
      repoProviders: [
        githubPlugin(
          cacheConnection: false,
          manageHooks: false,
          repoPermission: 'PULL'
        )
      ]
    )
  ])
])

////////////////////////////////////////////////////////////////////////////////

pipeline {
  agent { label 'built-in' }
  options {
    timestamps ()
    buildDiscarder(logRotator(artifactNumToKeepStr: '10', numToKeepStr: '100'))
  }
  stages {
    // For pollSCM
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
        //sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64 -o result-jetson-orin-agx-debug'
        //sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-nx-debug-from-x86_64  -o result-jetson-orin-nx-debug'
        //sh 'nix build -L .#packages.x86_64-linux.generic-x86_64-debug                     -o result-generic-x86_64-debug'
        //sh 'nix build -L .#packages.x86_64-linux.lenovo-x1-carbon-gen11-debug             -o result-lenovo-x1-carbon-gen11-debug'
        //sh 'nix build -L .#packages.riscv64-linux.microchip-icicle-kit-debug              -o result-microchip-icicle-kit-debug'
        //sh 'nix build -L .#packages.x86_64-linux.doc                                      -o result-doc'
      }
    }
    //stage('Build on aarch64') {
    //  steps {
    //    sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-agx-debug -o result-aarch64-jetson-orin-agx-debug'
    //    sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-nx-debug  -o result-aarch64-jetson-orin-nx-debug'
    //    sh 'nix build -L .#packages.aarch64-linux.imx8qm-mek-debug             -o result-aarch64-imx8qm-mek-debug'
    //    sh 'nix build -L .#packages.aarch64-linux.doc                          -o result-aarch64-doc'
    //  }
    //}
  }
}
