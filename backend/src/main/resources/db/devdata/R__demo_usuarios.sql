-- Contas de demonstracao. Senha de todas: Demo123!
INSERT INTO usuarios (nome, email, senha_hash, perfil) VALUES
    ('Ana Cliente', 'cliente@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'CLIENTE'),
    ('Cantina da Nona', 'restaurante@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'RESTAURANTE')
ON CONFLICT DO NOTHING;
