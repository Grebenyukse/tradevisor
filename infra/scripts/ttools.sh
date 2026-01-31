sudo apt update

# Install essential packages for enhanced terminal experience
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

# Install fzf for fuzzy finding (useful for history search)
sudo apt install -y fzf

# Install Oh My Bash for better bash experience (optional)
bash -c "$(curl -fsSL https://raw.githubusercontent.com/ohmybash/oh-my-bash/master/tools/install.sh)"