-- Carga de demonstracao, so para dev e para o ambiente publicado.
-- Senha de todas as contas: Demo123!
-- Um unico arquivo porque o Flyway roda os repeatable em ordem alfabetica, e os
-- restaurantes dependem dos usuarios.

INSERT INTO usuarios (nome, email, senha_hash, perfil) VALUES
    ('Ana Cliente', 'cliente@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'CLIENTE'),
    ('Nona Giulia', 'restaurante@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'RESTAURANTE'),
    ('Kazuo Tanaka', 'sushi@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'RESTAURANTE'),
    ('Bruno Lima', 'burger@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'RESTAURANTE'),
    ('Dona Cida', 'pf@demo.pedeja.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', 'RESTAURANTE')
ON CONFLICT DO NOTHING;

INSERT INTO restaurantes (usuario_id, nome, descricao, categoria, taxa_entrega, tempo_entrega_minutos, aberto)
SELECT u.id, r.nome, r.descricao, r.categoria, r.taxa, r.tempo, r.aberto
FROM (VALUES
    ('restaurante@demo.pedeja.local', 'Cantina da Nona', 'Pizzas de fermentação longa e massas da casa.', 'PIZZA', 6.90, 45, TRUE),
    ('sushi@demo.pedeja.local', 'Sushi Kazu', 'Combinados, temakis e pratos quentes.', 'JAPONESA', 8.50, 50, TRUE),
    ('burger@demo.pedeja.local', 'Burger do Bairro', 'Hambúrguer artesanal na chapa.', 'LANCHES', 4.99, 35, TRUE),
    ('pf@demo.pedeja.local', 'PF da Dona Cida', 'Comida caseira, prato feito todo dia.', 'BRASILEIRA', 0.00, 30, FALSE)
) AS r(email, nome, descricao, categoria, taxa, tempo, aberto)
JOIN usuarios u ON u.email = r.email
ON CONFLICT DO NOTHING;

INSERT INTO itens_cardapio (restaurante_id, nome, descricao, preco, disponivel)
SELECT r.id, i.nome, i.descricao, i.preco, i.disponivel
FROM (VALUES
    ('Cantina da Nona', 'Pizza Margherita', 'Molho de tomate, muçarela de búfala e manjericão.', 59.90, TRUE),
    ('Cantina da Nona', 'Pizza Calabresa', 'Calabresa artesanal, cebola roxa e azeitonas.', 54.90, TRUE),
    ('Cantina da Nona', 'Lasanha à Bolonhesa', 'Porção individual, 450 g.', 42.00, TRUE),
    ('Cantina da Nona', 'Tiramisù', 'Receita da casa.', 22.00, FALSE),
    ('Sushi Kazu', 'Combinado 20 peças', 'Sashimi, niguiri e uramaki do dia.', 79.90, TRUE),
    ('Sushi Kazu', 'Temaki de Salmão', 'Salmão, cream cheese e cebolinha.', 32.00, TRUE),
    ('Sushi Kazu', 'Yakisoba de Frango', 'Porção para uma pessoa.', 38.50, TRUE),
    ('Burger do Bairro', 'Clássico', 'Blend 160 g, queijo prato, alface, tomate e molho da casa.', 29.90, TRUE),
    ('Burger do Bairro', 'Bacon Duplo', 'Dois blends de 120 g, cheddar e bacon crocante.', 39.90, TRUE),
    ('Burger do Bairro', 'Batata Frita', 'Porção de 300 g.', 16.00, TRUE),
    ('PF da Dona Cida', 'PF de Frango', 'Arroz, feijão, frango grelhado, farofa e salada.', 24.90, TRUE),
    ('PF da Dona Cida', 'Feijoada', 'Só aos sábados.', 34.90, TRUE)
) AS i(restaurante, nome, descricao, preco, disponivel)
JOIN restaurantes r ON r.nome = i.restaurante
ON CONFLICT DO NOTHING;
