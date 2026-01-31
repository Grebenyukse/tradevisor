rm -rf /home/tradevisor/.oh-my-bash

sudo apt update

sudo apt install -y \
    bash-completion \
    command-not-found \
    curl \
    git \
    htop \
    jq \
    tree \
    vim \
    zsh

sudo apt install -y fzf

bash -c "$(curl -fsSL https://raw.githubusercontent.com/ohmybash/oh-my-bash/master/tools/install.sh)"