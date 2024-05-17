#!/usr/bin/env groovy

// SPDX-FileCopyrightText: 2024 Technology Innovation Institute (TII)
//
// SPDX-License-Identifier: Apache-2.0

////////////////////////////////////////////////////////////////////////////////

pipeline {
  agent any
  triggers {
    pollSCM '* * * * *'
  }
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder logRotator(artifactNumToKeepStr: '10', numToKeepStr: '100')
  }
  stages {
    stage('Checkout') {
      steps {
        dir('ghaf') {
          checkout scmGit(
            branches: [[name: 'main']],
            extensions: [cleanBeforeCheckout()],
            userRemoteConfigs: [[url: 'https://github.com/tiiuae/ghaf.git']]
          )
        }
      }
    }
    stage("Build") {
      parallel {
        stage('x86_64') {
          steps {
            dir('ghaf') {
              sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64'
              sh 'nix build -L .#packages.x86_64-linux.nvidia-jetson-orin-nx-debug-from-x86_64'
              sh 'nix build -L .#packages.x86_64-linux.lenovo-x1-carbon-gen11-debug'
              sh 'nix build -L .#packages.riscv64-linux.microchip-icicle-kit-debug'
              sh 'nix build -L .#packages.x86_64-linux.doc'
            }
          }
        }
        stage('aarch64') {
          steps {
            dir('ghaf') {
              sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-agx-debug'
              sh 'nix build -L .#packages.aarch64-linux.nvidia-jetson-orin-nx-debug'
              sh 'nix build -L .#packages.aarch64-linux.doc'
            }
          }
        }
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
