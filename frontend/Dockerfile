# ============================================================
# Frontend estatico servido por nginx alpine.
# Sem build step - HTML/CSS/JS puro.
# ============================================================
FROM nginx:alpine

# Remove a pagina default e copia os assets do projeto
RUN rm -rf /usr/share/nginx/html/*
COPY . /usr/share/nginx/html

# Configuracao nginx customizada (compressao + cache)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
