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
    stage('Build x86_64') {
      steps {
        dir('ghaf') {
          nix_build('.#packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64')
          nix_build('.#packages.x86_64-linux.nvidia-jetson-orin-nx-debug-from-x86_64')
          nix_build('.#packages.x86_64-linux.lenovo-x1-carbon-gen11-debug')
          nix_build('.#packages.riscv64-linux.microchip-icicle-kit-debug')
          nix_build('.#packages.x86_64-linux.doc')
        }
      }
    }
    stage('Build aarch64') {
      steps {
        dir('ghaf') {
          nix_build('.#packages.aarch64-linux.nvidia-jetson-orin-agx-debug')
          nix_build('.#packages.aarch64-linux.nvidia-jetson-orin-nx-debug')
          nix_build('.#packages.aarch64-linux.doc')
        }
      }
    }
  }
  post {
    always {
      set_result()
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
