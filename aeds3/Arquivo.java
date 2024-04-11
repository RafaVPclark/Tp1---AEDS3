package aeds3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Arquivo<T extends Registro> {

  // protected static int TAM_CABECALHO = 4;
  // Como 12 para long do endereço da próxima lapide
  protected static int TAM_CABECALHO = 12;
  protected RandomAccessFile arquivo;
  protected HashExtensivel<ParIDEndereco> indiceDireto;
  private String nomeEntidade;
  private Constructor<T> construtor;

  public Arquivo(String na, Constructor<T> c) throws Exception {
    this.nomeEntidade = na;
    this.construtor = c;
    arquivo = new RandomAccessFile("dados/" + this.nomeEntidade + ".db", "rwd");
    if (arquivo.length() < TAM_CABECALHO) {
      arquivo.seek(0);
      arquivo.writeInt(0);
      arquivo.writeLong(0);
    }
    indiceDireto = new HashExtensivel<>(ParIDEndereco.class.getConstructor(),
        4, "dados/" + this.nomeEntidade + ".hash_d.db",
        "dados/" + this.nomeEntidade + ".hash_c.db");
  }

  public int create(T obj) throws Exception {
    int ultimoID;
    arquivo.seek(0);
    ultimoID = arquivo.readInt();
    ultimoID++;
    arquivo.seek(0);
    arquivo.writeInt(ultimoID);
    obj.setID(ultimoID);

    long endereco;
    long enderecoEndereco;

    arquivo.seek(4);
    enderecoEndereco = arquivo.getFilePointer(); // endereço do endereço
    endereco = arquivo.readLong(); // pega o endereço do cabeçalho

    byte[] ba = obj.toByteArray(); // cria o objeto

    if (endereco != 0) // se o endereço != 0 (se ele apontar para alguém)
    {
      while (true) // while true kkkkkkkkk
      {
        arquivo.seek(endereco + 1); // vai pro endereço excluido (pulando lápide)
        short tam = arquivo.readShort(); // pega o tamanho

        if (tam * 0.8 <= ba.length && tam > ba.length) { // verifica se cabe no registro excluido e representa 80% dos
                                                         // espaços vazios
          long enderecoTmp = arquivo.readLong(); // leio o proximo excluido
          arquivo.seek(enderecoEndereco); // volto ao ponteiuro anterior
          arquivo.writeLong(enderecoTmp); // reescrevo o excluido
          break; // caso caiba e esteja em 80%, sai do loop
        }

        enderecoEndereco = arquivo.getFilePointer();
        endereco = arquivo.readLong(); // ler o proximo endereço excluido

        if (endereco == 0) // se endereço é o ultimo excluido
        {
          endereco = arquivo.length(); // endereço recebe final do arquivo
          break;
        }
      }
    } else {
      endereco = arquivo.length();
    }

    arquivo.seek(endereco);
    arquivo.writeByte(' '); // escreve lapide
    arquivo.writeShort(ba.length); // escreve tamanho
    arquivo.write(ba); // escreve o objeto

    indiceDireto.create(new ParIDEndereco(ultimoID, endereco));
    return obj.getID();
  }

  public T read(int id) throws Exception {
    T obj = construtor.newInstance();
    int tam;
    long endereco = indiceDireto.read(id).getEndereco();
    if (endereco > 0) {
      arquivo.seek(endereco + 1); // pulando o campo lápide
      tam = arquivo.readShort();
      byte[] ba = new byte[tam];
      arquivo.read(ba);
      obj.fromByteArray(ba);
      return obj;
    } else
      return null;
  }

  public boolean update(T objAtualizado) throws Exception {
    long endereco = indiceDireto.read(objAtualizado.getID()).getEndereco();
    if (endereco > 0) {
      // Lê o registro atual
      arquivo.seek(endereco + 1); // pula o lápide
      int tamAntigo = arquivo.readShort();
      byte[] baAntigo = new byte[tamAntigo];
      arquivo.read(baAntigo);
      T objAntigo = construtor.newInstance();
      objAntigo.fromByteArray(baAntigo);
      
      // Se o novo registro couber no mesmo local
      byte[] baAtualizado = objAtualizado.toByteArray();
      // Verifica apenas o registro antigo do id passado
      if( baAtualizado.length < tamAntigo){
        arquivo.seek(endereco + 1 + 2);
        arquivo.write(baAtualizado);
      }
      else{
        this.delete(objAtualizado.getID());
        // Vai para o inicio do arquivo
        arquivo.seek(4);
        long enderecoEndereco = arquivo.getFilePointer(); // endereço do endereço
        long endereco2 = arquivo.readLong(); // onde está o endereço da próxima lápide
        if (endereco2 != 0) // se o endereço != 0 (se ele apontar para alguém)
        {
          while (true) // while true kkkkkkkkk
          {
            arquivo.seek(endereco2 + 1); // vai pro endereço excluido (pulando lápide)
            short tam = arquivo.readShort(); // pega o tamanho
  
            if (baAtualizado.length < tam) { // verifica se cabe no registro excluido
                                                                                 // e representa 80% dos
              // espaços vazios
              long enderecoTmp = arquivo.readLong(); // leio o proximo excluido
              arquivo.seek(enderecoEndereco); // volto ao ponteiuro anterior
              arquivo.writeLong(enderecoTmp); // reescrevo o excluido
              break; // caso caiba e esteja em 80%, sai do loop
            }
  
            enderecoEndereco = arquivo.getFilePointer();
            endereco2 = arquivo.readLong(); // ler o proximo endereço excluido
  
            if (endereco2 == 0) // se endereço é o ultimo excluido
            {
              endereco2 = arquivo.length(); // endereço recebe final do arquivo
              break;
            }
          }
        } else {
          endereco2 = arquivo.length();
        }
        arquivo.seek(endereco2);
        arquivo.writeByte(' '); // escreve lapide
        arquivo.writeShort(baAtualizado.length); // escreve tamanho
        arquivo.write(baAtualizado); // escreve o objeto
  
        // Atualiza o índice direto
        indiceDireto.update(new ParIDEndereco(objAtualizado.getID(), endereco2));
      
      }
      
      return true;
    }else{
      return false;
    }

  }

  public boolean delete(int id) throws Exception {
    long endereco = indiceDireto.read(id).getEndereco(); // pega o endereço do registro para excluir na tabela hash
    arquivo.seek(4); // pula o id no cabeçalho (primeiro excluido)
    long enderecoUltimo = arquivo.readLong(); // lê o endereço do proximo excluido

    if (enderecoUltimo == 0) // se cabeçalho == 0
    {
      arquivo.seek(4); // volta no cabeçalho
      arquivo.writeLong(endereco); // escreve o endereço do registro excluido
    } else {
      while (enderecoUltimo != 0) {
        arquivo.seek(enderecoUltimo + 3); // vai pro registro excluido (pula lapide e tamanho)
        long proximo = arquivo.readLong(); // lê o proximo registro excluido
        if (proximo == 0) // se proximo == 0
        {
          arquivo.seek(enderecoUltimo + 3);
          arquivo.writeLong(endereco);
          break;
        }
        enderecoUltimo = proximo;
      }
    }

    if (endereco > 0) {
      arquivo.seek(endereco);
      arquivo.writeByte('*');
      arquivo.seek(endereco + 3);
      arquivo.writeLong(0);
      indiceDireto.delete(id);
      return true;
    } else
      return false;

  }

  public void close() throws Exception {
    arquivo.close();
  }

  // REORGANIZAR - VERSÃO QUE REORDENA O ARQUIVO, USANDO INTERCALAÇÃO BALANCEADA
  // Recebe um objeto vazio para auxiliar na reorganização
  @SuppressWarnings("unchecked")
  public void reorganizar() throws Exception {

    // Lê o cabeçalho
    byte[] ba_cabecalho = new byte[TAM_CABECALHO];
    arquivo.seek(0);
    arquivo.read(ba_cabecalho);

    // ---------------------------------------------------------------------
    // Primeira etapa (distribuição)
    // ---------------------------------------------------------------------
    int tamanhoBlocoMemoria = 3;
    List<T> registrosOrdenados = new ArrayList<>();

    int contador = 0, seletor = 0;
    short tamanho;
    byte lapide;
    byte[] dados;
    T r1 = construtor.newInstance(),
        r2 = construtor.newInstance(),
        r3 = construtor.newInstance();
    T rAnt1, rAnt2, rAnt3;

    // Abre três arquivos temporários para escrita (1º conjunto)
    DataOutputStream out1 = new DataOutputStream(new FileOutputStream("dados/temp1.db"));
    DataOutputStream out2 = new DataOutputStream(new FileOutputStream("dados/temp2.db"));
    DataOutputStream out3 = new DataOutputStream(new FileOutputStream("dados/temp3.db"));
    DataOutputStream out = null;

    try {
      contador = 0;
      seletor = 0;
      while (true) {

        // Lê o registro no arquivo de dados
        lapide = arquivo.readByte();
        tamanho = arquivo.readShort();
        dados = new byte[tamanho];
        arquivo.read(dados);
        r1.fromByteArray(dados);

        // Adiciona o registro ao vetor
        if (lapide == ' ') {
          registrosOrdenados.add((T) r1.clone());
          contador++;
        }
        if (contador == tamanhoBlocoMemoria) {

          switch (seletor) {
            case 0:
              out = out1;
              break;
            case 1:
              out = out2;
              break;
            default:
              out = out3;
          }
          seletor = (seletor + 1) % 3;

          Collections.sort(registrosOrdenados);
          for (T r : registrosOrdenados) {
            dados = r.toByteArray();
            out.writeShort(dados.length);
            out.write(dados);
          }
          registrosOrdenados.clear();

          contador = 0;
        }

      }

    } catch (EOFException eof) {
      // Descarrega os últimos registros lidos
      if (contador > 0) {
        switch (seletor) {
          case 0:
            out = out1;
            break;
          case 1:
            out = out2;
            break;
          default:
            out = out3;
        }

        Collections.sort(registrosOrdenados);
        for (T r : registrosOrdenados) {
          dados = r.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
        }
      }
    }
    out1.close();
    out2.close();
    out3.close();

    // ---------------------------------------------------------------------
    // Segunda etapa (intercalação)
    // ---------------------------------------------------------------------
    DataInputStream in1, in2, in3;
    boolean sentido = true; // true: conj1 -> conj2 | false: conj2 -> conj1
    boolean maisIntercalacoes = true;
    boolean compara1, compara2, compara3;
    boolean terminou1, terminou2, terminou3;

    while (maisIntercalacoes) {

      maisIntercalacoes = false;
      compara1 = false;
      compara2 = false;
      compara3 = false;
      terminou1 = false;
      terminou2 = false;
      terminou3 = false;

      // Seleciona as fontes e os destinos
      if (sentido) {
        in1 = new DataInputStream(new FileInputStream("dados/temp1.db"));
        in2 = new DataInputStream(new FileInputStream("dados/temp2.db"));
        in3 = new DataInputStream(new FileInputStream("dados/temp3.db"));
        out1 = new DataOutputStream(new FileOutputStream("dados/temp4.db"));
        out2 = new DataOutputStream(new FileOutputStream("dados/temp5.db"));
        out3 = new DataOutputStream(new FileOutputStream("dados/temp6.db"));
      } else {
        in1 = new DataInputStream(new FileInputStream("dados/temp4.db"));
        in2 = new DataInputStream(new FileInputStream("dados/temp5.db"));
        in3 = new DataInputStream(new FileInputStream("dados/temp6.db"));
        out1 = new DataOutputStream(new FileOutputStream("dados/temp1.db"));
        out2 = new DataOutputStream(new FileOutputStream("dados/temp2.db"));
        out3 = new DataOutputStream(new FileOutputStream("dados/temp3.db"));
      }
      sentido = !sentido;
      seletor = 0;

      // novos registros anteriores vazios
      r1 = construtor.newInstance();
      r2 = construtor.newInstance();
      r3 = construtor.newInstance();

      // Inicia a intercalação dos segmentos
      boolean mudou1 = true, mudou2 = true, mudou3 = true;
      while (!terminou1 || !terminou2 || !terminou3) {

        if (!compara1 && !compara2 && !compara3) {
          // Seleciona o próximo arquivo de saída
          switch (seletor) {
            case 0:
              out = out1;
              break;
            case 1:
              out = out2;
              break;
            default:
              out = out3;
          }
          seletor = (seletor + 1) % 3;

          if (!terminou1)
            compara1 = true;
          if (!terminou2)
            compara2 = true;
          if (!terminou3)
            compara3 = true;
        }

        // le o próximo registro da última fonte usada
        if (mudou1) {
          rAnt1 = (T) r1.clone();
          try {
            tamanho = in1.readShort();
            dados = new byte[tamanho];
            in1.read(dados);
            r1.fromByteArray(dados);
            if (r1.compareTo(rAnt1) < 0)
              compara1 = false;
          } catch (EOFException e) {
            compara1 = false;
            terminou1 = true;
          }
          mudou1 = false;
        }
        if (mudou2) {
          rAnt2 = (T) r2.clone();
          try {
            tamanho = in2.readShort();
            dados = new byte[tamanho];
            in2.read(dados);
            r2.fromByteArray(dados);
            if (r2.compareTo(rAnt2) < 0)
              compara2 = false;
          } catch (EOFException e) {
            compara2 = false;
            terminou2 = true;
          }
          mudou2 = false;
        }
        if (mudou3) {
          rAnt3 = (T) r3.clone();
          try {
            tamanho = in3.readShort();
            dados = new byte[tamanho];
            in3.read(dados);
            r3.fromByteArray(dados);
            if (r3.compareTo(rAnt3) < 0)
              compara3 = false;
          } catch (EOFException e) {
            compara3 = false;
            terminou3 = true;
          }
          mudou3 = false;
        }

        // Escreve o menor registro
        if (compara1 && (!compara2 || r1.compareTo(r2) <= 0) && (!compara3 || r1.compareTo(r3) <= 0)) {
          dados = r1.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou1 = true;
        } else if (compara2 && (!compara1 || r2.compareTo(r1) <= 0) && (!compara3 || r2.compareTo(r3) <= 0)) {
          dados = r2.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou2 = true;
        } else if (compara3 && (!compara1 || r3.compareTo(r1) <= 0) && (!compara2 || r3.compareTo(r2) <= 0)) {
          dados = r3.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou3 = true;
        }

        // Testa se há mais intercalações a fazer
        if (seletor > 1)
          maisIntercalacoes = true;
      }

      in1.close();
      in2.close();
      in3.close();
      out1.close();
      out2.close();
      out3.close();
    }

    // return;

    // copia os registros de volta para o arquivo original
    if (sentido)
      in1 = new DataInputStream(new FileInputStream("dados/temp1.db"));
    else
      in1 = new DataInputStream(new FileInputStream("dados/temp4.db"));

    arquivo.close();
    new File("dados/" + nomeEntidade + ".db").delete();
    DataOutputStream ordenado = new DataOutputStream(new FileOutputStream(nomeEntidade));
    ordenado.write(ba_cabecalho);

    indiceDireto.close();
    new File("dados/" + nomeEntidade + ".hash_d.db").delete();
    new File("dados/" + nomeEntidade + ".hash_c.db").delete();
    indiceDireto = new HashExtensivel<>(ParIDEndereco.class.getConstructor(),
        4, "dados/" + this.nomeEntidade + ".hash_d.db",
        "dados/" + this.nomeEntidade + ".hash_c.db");

    long endereco;
    try {
      while (true) {
        tamanho = in1.readShort();
        dados = new byte[tamanho];
        in1.read(dados);
        r1.fromByteArray(dados);

        endereco = ordenado.size();
        ordenado.writeByte(' '); // lápide
        ordenado.writeShort(tamanho);
        ordenado.write(dados);
        indiceDireto.create(new ParIDEndereco(r1.getID(), endereco));
      }
    } catch (EOFException e) {
      // saída normal
    }
    ordenado.close();
    in1.close();
    (new File("dados/temp1.db")).delete();
    (new File("dados/temp2.db")).delete();
    (new File("dados/temp3.db")).delete();
    (new File("dados/temp4.db")).delete();
    (new File("dados/temp5.db")).delete();
    (new File("dados/temp6.db")).delete();
    arquivo = new RandomAccessFile("dados/" + this.nomeEntidade + ".db", "rw");
  }

}
