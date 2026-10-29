# Gerador de etiquetas

Aplicativo web local que preenche o modelo `6.docx` com um texto escolhido e
gera uma cópia pronta para baixar. O documento gerado mantém a página, a tabela,
as dimensões das células e a formatação do arquivo original; apenas o texto das
120 etiquetas é substituído.

## Como usar

1. Instale o Python 3, se ainda não estiver instalado.
2. No diretório do projeto, execute `python3 app.py`.
3. O terminal mostra o endereço para abrir no celular, por exemplo
   `http://192.168.1.25:8000`.
4. Conecte computador e celular à mesma rede Wi-Fi e abra esse endereço no
   navegador do celular. Se solicitado, permita o acesso do Python na rede
   privada do computador.
5. Digite o texto e selecione **Gerar e baixar etiquetas**.

O texto é repetido nas 120 etiquetas (6 colunas por 20 linhas). Textos curtos
tendem a caber melhor na área de impressão. O aplicativo não envia os dados
para serviços externos e continua gerando arquivos `.docx`. Ele aceita
conexões da rede local para permitir o acesso pelo celular: use apenas em uma
rede Wi-Fi confiável e não configure o roteador para expor a porta 8000 à
internet. O computador precisa continuar ligado enquanto o aplicativo estiver
em uso.

## APK Android

O projeto Android fica na pasta `app/`. Ele incorpora o modelo e gera o mesmo
arquivo `.docx` diretamente no celular, sem internet nem computador na rede.
O botão **Salvar arquivo .docx** abre o seletor de local do Android; **Compartilhar
arquivo** abre o menu de compartilhamento para enviar o documento por aplicativos
como o WhatsApp.

Para compilar um APK de depuração com Android SDK instalado, execute:

```sh
./gradlew assembleDebug
```

O APK gerado estará em `app/build/outputs/apk/debug/app-debug.apk`. Para
instalá-lo, transfira o arquivo ao Android e autorize a instalação do app de
origem quando solicitado. O APK de depuração serve para testes; uma distribuição
pública requer assinatura de lançamento.

## Testes

Execute `python3 -m unittest` para a versão web e
`./gradlew testDebugUnitTest` para a geração de documentos na versão Android.
