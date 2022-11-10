# Sistemas Distribuídos - Trabalho 2

## Alunos

- ANA CAROLINA FERREIRA
- FELIPE FAHRION
- VINICIUS JAGGI
- ROBSON FELIPE BITTENCOURT


O segundo trabalho da disciplina consiste em desenvolver um programa distribuído que implemente relógios vetoriais para ordenaçao de eventos (com ordem total). 
O programa deve receber como entrada um arquivo de configuraçao e um numero que identifica uma das linhas do arquivo de configuraçao. 
Todos os processos devem possuir uma copia desse arquivo. Cada linha do arquivo de configuraçao terao o seguinte formato:

```
id host port chance events min_delay max_delay
```

-  id é um numero inteiro que identifica o processo;

-  host é o hostname ou endereco IP da maquina (nodo) que executa o processo;
-  port é o numero da porta que o processo vai escutar;
-  chance é uma probabilidade (entre 0 e 1) da ocorrencia de um evento de envio de mensagem. Por exemplo, o valor 0.2 significa 20% de probabilidade de ser realizado um envio de mensagem, sendo os 80% restantes eventos locais;
-  events é o numero de eventos que serao executados nesse nodo (recomenda-se aproximadamente 100 eventos);
-  min delay é o tempo minimo de intervalo entre eventos (recomenda-se valores entre 100 e 300 ms);
-  max delay é o tempo maximo de intervalo entre eventos (recomenda-se valores entre 350 e 750 ms).

Execução do programa e descrição do algoritmo:
 ́E importante que um mecanismo de sincronização inicial seja implementado para que todos os processos iniciem a execução do algoritmo ao mesmo
tempo. Para isso, pode-se utilizar um grupo multicast. Cada nodo pode executar um evento local ou enviar uma mensagem para outro nodo, de acordo com a probabilidade estabelecida em sua configuração. Utilize datagramas
para o envio de mensagens.
-  Para eventos locais, incremente o relógio local;
-  Para envio de mensagens, escolha aleatoriamente entre um dos outros
nodos definidos no arquivo de configuração e envie uma mensagem contendo também o valor do relógio.
Devem ser gerados eventos com intervalo de min delay a max delay, e cada nodo deve executar diversos desses eventos (de acordo com a configuração,
podendo ser um evento local ou envio de mensagem) e depois terminar sua
execução. Pode ocorrer de um nodo tentar enviar uma mensagem para um
nodo que j ́a terminou sua execução, neste caso trate o erro de tal forma que
interrompa a execução do processo que tentou enviar.
Saída do programa:
Cada processo deve individualmente produzir sua própria saída. Use a
seguinte sintaxe para formatar a saída:
-  Evento local: i [c,c,c,c,...] L, onde i  ́e o ID do nodo local e [c,c,c,c,...]
 ́e o valor do relógio vetorial local;
-  Envio de mensagem: i [c,c,c,c,...] S d, onde i  ́e o ID do nodo local,
[c,c,c,c,...]  ́e o valor do relógio vetorial enviado e d  ́e o ID do nodo
destinat ́ario da mensagem;
-  Recebimento de mensagem: i [c,c,c,c,...] R s t, onde i  ́e o ID do nodo
local, [c,c,c,c,...]  ́e o valor do relógio vetorial depois do recebimento
da mensagem, s  ́e ID do nodo remetente da mensagem e t  ́e o valor do
relógio lógico recebido com a mensagem.

### Como rodar

```
java App <config_file.json> <process_id>

```

### Exemplos

```
java App config_sample.txt 0

```