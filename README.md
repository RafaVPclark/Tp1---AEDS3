# Tp1---AEDS3
Alunos:
Rafael Vilela Padilha Clark
Igor Silva de Paiva Paschoalino
O trabalho em questão é um código desenvolvido pelo professor Kutova juntamente com os alunos para representar um banco de dados de armazenamento de livros, com algumas características como, id, isbn , título, etc. Nele, são feitos relacionamentos entre as informações dos livros e autores, entre outros. Para esse trabalho, foi necessário realizar um CRUD( Create, Read, Update, Delete) em arquivos, para gerenciar os dados. Os dados em questão deverão ser escritos no arquivo livros.db, no qual são transformados em bytes e escritos de forma indexada no arquivo. Para esse trabalho, nós aumentamos o tamanho do cabeçalho para 12 bytes, no qual 4 bytes serão separados para encontrar o último id escrito no arquivo, e o restante para apontar para a posição do primeiro registro excluído, realizando uma cadeia em sequência para encontrar todos os registros com as lápides marcadas como excluídas. Esse sistema, foi implementado para facilitar as operações de crud, reaproveitando os espaços dessas lápides excluídas otimizando o espaço utilizado ao máximo. O grande desafio para esse trabalho, foi reutilizar o espaço corretamente de um registro deletado. Essa reutilização deve ocorrer em duas ocasiões. Quando um novo registro for criado, o código precisa buscar uma lápide de um registro deletado e verificar se é possível reutilizar o espaço dele para o novo registro criado, seguindo uma regra de que só poderia ser utilizado aquele espaço deletado se o novo registro tivesse entre 80% e 100% do espaço total do registro anterior. O mesmo deve ocorrer na operação de update, no qual se o tamanho do registro antigo for menor que o registro novo, realizar essa busca de lápides novamente, verificar a disponibilidade de espaço e, se nenhuma lápide possuir disponibilidade escrever um novo registro no fim do arquivo.

Experiência do grupo:
Os requisitos foram implementados todos. O código realiza uma realocação de espaço eficaz nos cenários especificados.
Acredito que o mais difícil desse trabalho foi compreender o código e sua lógica num gráu maior de abstração, uma vez que para realizar um create por exemplo era necessário imaginar de que forma os outros métodos seriam afetados e vice versa. O Tp em si não era complicado. Nós entramos em ligação através do discord e fomos resolvendo os casos um por um porém a compreensão mais teórica foi a principal dificuldade.

O que você considerou como perda aceitável para o reuso de espaços vazios, isto é, quais são os critérios para a gestão dos espaços vazios? O registro novo precisa ocupar mais de 80% e menos de 100% do registro antigo para ocupar o seu espaço.
O código do CRUD com arquivos de tipos genéricos está funcionando corretamente? Sim
O CRUD tem um índice direto implementado com a tabela hash extensível? Sim
A operação de inclusão busca o espaço vazio mais adequado para o novo registro antes de acrescentá-lo ao fim do arquivo? Sim
A operação de alteração busca o espaço vazio mais adequado para o registro quando ele cresce de tamanho antes de acrescentá-lo ao fim do arquivo? Sim
As operações de alteração (quando for o caso) e de exclusão estão gerenciando os espaços vazios para que possam ser reaproveitados? Sim
O trabalho está funcionando corretamente? Sim 
O trabalho está completo? Sim
O trabalho é original e não a cópia de um trabalho de um colega? Sim
