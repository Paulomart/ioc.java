workflow "Build on commit" {
  on = "push"
  resolves = ["upload maven repo"]
}

action "build" {
  uses = "bas/mvn@master"
  args = "clean install"
}

# Filter for master branch
action "check if branch is master" {
  needs = "build"
  uses = "actions/bin/filter@master"
  args = "branch master"
}

action "download maven repo" {
  uses = "maxheld83/rsync@v0.1.1"
  needs = "check if branch is master"
  secrets = [
    "SSH_PRIVATE_KEY",
    "SSH_PUBLIC_KEY",
    "HOST_NAME",
    "HOST_IP",
    "HOST_FINGERPRINT",
  ]
  args = [
    "deploy_user@$HOST_NAME:/var/www/maven_repo/",
    "$GITHUB_WORKSPACE/maven_repo",
  ]
}

action "delete .ssh folder" {
  needs = "download maven repo"
  uses = "actions/bin/sh@master"
  args = ["rm -fr /github/home/.ssh"]
}

action "deploy to local repo" {
  needs = "delete .ssh folder"
  uses = "bas/mvn@master"
  args = "deploy -DaltDeploymentRepository=snapshot-repo::default::file:$GITHUB_WORKSPACE/maven_repo"
}

action "upload maven repo" {
  uses = "maxheld83/rsync@v0.1.1"
  needs = "deploy to local repo"
  secrets = [
    "SSH_PRIVATE_KEY",
    "SSH_PUBLIC_KEY",
    "HOST_NAME",
    "HOST_IP",
    "HOST_FINGERPRINT",
  ]
  args = [
    "$GITHUB_WORKSPACE/maven_repo",
    "deploy_user@$HOST_NAME:/var/www/",
  ]
}
