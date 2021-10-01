FROM squidfunk/mkdocs-material
RUN pip install \
    mkdocs-gen-files \
    mkdocs-exclude \
    mike \
    pymdown-extensions && \
    git config --global credential.helper 'store --file=/root/.git-credentials'