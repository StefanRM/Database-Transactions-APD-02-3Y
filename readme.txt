.___________. _______ .___  ___.      ___          ___   
|           ||   ____||   \/   |     /   \        |__ \  
`---|  |----`|  |__   |  \  /  |    /  ^  \          ) | 
    |  |     |   __|  |  |\/|  |   /  /_\  \        / /  
    |  |     |  |____ |  |  |  |  /  _____  \      / /_  
    |__|     |_______||__|  |__| /__/     \__\    |____| 

Nume:    Maftei Stefan - Radu
Grupa:   336CC
Materie: APD


=== Implementarea ===
	Implementarea bazei de date este realizata in clasa Database care
implementeaza interfata MyDatabase oferita in schelet.
	O baza de date contine mai multe tabele, astfel am realizat o clasa Table
care contine proprietatile unei tabele, iar in Database se utilizeaza un
HashMap<String, Table> pentru acces rapid a tabelei in functie de numele ei.
O tabela contine mai multe coloane, astfel am realizat o clasa Column care
contine proprietatile unei coloane, iar in Table se utilizeaza o lista de
coloane.
	Pentru mecanismul de functionare si sincronizare am utilizat paradigma
cititori-scriitori prezentata in cadrul cursului. Astfel pot fi una sau mai
multe procese "cititori" (care doar citesc din memorie) si niciun proces
"scriitor" (care scrie in memorie) sau niciun proces "cititor" si doar un
singur proces "scriitor". Fiecare tabela contine semafoarele necesare pentru
implementarea acestei paradigme, iar in clasa Database functiile "cititori"
(select()) si "scriitori" (update() si insert()) folosesc membrii tabelei
respective pentru a implementa paradigma cititori-scriitori.
	Metodele clasei Database:
1. convertToDatabaseType()
	- tipurile elementelor sunt convertite din tipul limbajului Java in tipul
bazei de date.

2. select()
	- pentru fiecare operatie se calculeaza o lista cu indicii care indeplinesc
conditia;
	- se trimite lista spre a fi calculata operatia.

3. update()
	- se calculeaza o lista cu indicii care indeplinesc conditia;
	- fiecare element aflat la indicele calculat in lista cu indici este
inlocuit cu valoarea data.

4. insert()
	- se verifica tipul elementelor de adaugat, daca se potrivesc se adauga
pe coloana, daca nu se sterge ce s-a adaugat pana atunci si se intoarce o
exceptie.

5. startTransaction()
	- se blocheaza tabela pentru a fi citita/scrisa doar de thread-ul care a
initiat aceasta tranzactie.

6. endTransaction()
	- se deblocheaza tabela, apel utilizat dupa startTransaction() pentru
marcarea incheierii tranzactiei.

7. createTable()
	- adauga o tabela noua in baza de date.

8. initDB() si stopDB()
	- se initializeaza numarul de procese worker ale bazei de date, respectiv
se inchid aceste procese.

	Metodele select() si update() cauta in tabela elementele care satisfac o
conditie. Astfel aceasta cautare este realizata in paralel cu ajutorul
worker-ilor bazei de date. Metoda aceasta in care se calculeaza indicii este:
9. getIndexOfConditionSatisfaction()
	- se obtin informatiile din conditie si apoi se imparte in mod egal
worker-ilor task-ul/job-ul de a obtine o lista cu indici pe portiunea de date
care le-a fost asignata;
	- pentru sincronizare se utilizeaza o bariera care asteapta ca job-urile
worker-ilor sa fie terminate pentru ca apoi sa se asambleze lista finala de
indici care este trimisa la select() pentru a fi trimisa apoi spre calcularea
operatiei si adaugarea in lista de rezultate.

	Paralelizarea cautarii indicilor este realizata cu ExecutorService.
Task-ul pe care il executa se afla in clasa MyTask care implementeaza
interfata Runnable. In constructorul clasei MyTask se obtin toate valorile
necesare pentru efectuarea run()-ului. In run() se parcurge lista valorilor
coloanei doar intre limitele start si stop calculate in constructor, apoi se
verifica daca este satisfacuta conditia si se adauga in lista de indici.
La final se afla o bariera pentru a sincroniza toti worker-ii si database-ul.
Fiecare worker va astepta la bariera sa termine ceilalti, iar baza de date
asteapta si ea la aceeasi bariera ca worker-ii sa fie gata. Apoi baza de date
asambleaza lista finala de indici din listele oferite de workeri.


=== Compilarea ===
	Am realizat compilarea si impachetarea operatiei in fisierul build.xml,
pentru sistemul de build Ant.
	Regulile:
1. run - ruleaza aplicatia;
2. jar - impacheteaza aplicatia intr-un fisier database.jar;
3. compile - compileaza aplicatia in folder ./classes;
4. clean - sterge folder-ul ./classes si fisierul database.jar.


=== Testarea ===
	Am realizat testarea pe cluster pe coada "campus-haswell.q". Timpii
obtinuti in urma testarii sunt:

+----------------------------------------------------------+
| Nr. threads |    Insert    |    Update    |    Select    |
|----------------------------------------------------------|
|             |   25391 ms   |    377 ms    |    2658 ms   |
|             ---------------------------------------------|
|             |   19286 ms   |    347 ms    |    2934 ms   |
|             ---------------------------------------------|
|      1      |   19121 ms   |    366 ms    |    2720 ms   |
|             ---------------------------------------------|
|             |   21946 ms   |    352 ms    |    2341 ms   |
|             ---------------------------------------------|
|             |   21542 ms   |    358 ms    |    3000 ms   |
|             ---------------------------------------------|
|             |   24799 ms   |    352 ms    |    2953 ms   |
|----------------------------------------------------------|
|  Media->    | 22014.167 ms |   358.667 ms |  2767.667 ms |
|__________________________________________________________|
|             |    8724 ms   |    266 ms    |    1149 ms   |
|             ---------------------------------------------|
|             |   12784 ms   |    280 ms    |    2170 ms   |
|             ---------------------------------------------|
|      2      |   18923 ms   |    272 ms    |    1640 ms   |
|             ---------------------------------------------|
|             |   12719 ms   |    255 ms    |    2282 ms   |
|             ---------------------------------------------|
|             |   17118 ms   |    285 ms    |    1505 ms   |
|             ---------------------------------------------|
|             |   15673 ms   |    258 ms    |    1409 ms   |
|----------------------------------------------------------|
|  Media->    |  14323.5 ms  |   269.333 ms |   1692.5 ms  |
|__________________________________________________________|
|             |    6997 ms   |    216 ms    |    1017 ms   |
|             ---------------------------------------------|
|             |    5391 ms   |    209 ms    |    1538 ms   |
|             ---------------------------------------------|
|      4      |   15878 ms   |    240 ms    |    1105 ms   |
|             ---------------------------------------------|
|             |    8010 ms   |    237 ms    |    1844 ms   |
|             ---------------------------------------------|
|             |   13587 ms   |    230 ms    |    1047 ms   |
|             ---------------------------------------------|
|             |    4964 ms   |    159 ms    |     952 ms   |
|----------------------------------------------------------|
|  Media->    |  9137.833 ms |   215.167 ms |   1250.5 ms  |
+----------------------------------------------------------+

	Astfel implementarea bazei de date cu functiile sale scaleaza in
functie de numarul de thread-uri utilizat.
