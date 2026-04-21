

## Iaşi,2022
UNIVERSITATEATEHNICĂ„GheorgheAsachi”dinIAŞI
## FACULTATEADEAUTOMATICĂŞICALCULATOARE
DOMENIUL:Calculatoareşitehnologiainformaţiei
SPECIALIZAREA:Tehnologiainformaţiei
SistemeDistribuite-
## Laborator6
MicroserviciiWebcuSpringBootînKotlin



## Laborator6
## 1
Laborator6SD:MicroserviciiWebcuSpringBootînKotlin
## Introducere
## Aplicațiimonolitice
## Primeleaplicațiieraudedimensiunimici,iarlogicadeprezentareeralaunlocculogica
debusiness-vezimodelulcudouăsautreiniveluridesprecareamdiscutat.Elesuntlegatede
vremeacândaplicațiaeraexecutatăpeosingurămașină.Nuuitațicăacesttermenafostintodus
deabiaînepocamicroserviciilorșianoruluișideaceeaelsereferălaomarevarietatede
implementărialeaplicațiilor.Deșigestiunealorșimaialesdepanareaeramaiușoarădatorită
centralizăriiesteclarcăastfeldeabordărinusuntpotrivitepentrunor.Uniiinginericonsiderăcă
suntușordereplicat-iaracestăpercepțieestedatoratăneînțelegeriiideiidinspatelemultiplicării
(adicădereducereazonelordesupraîncărcarespecificeuneiastfeldeaplicații)defaptleeste
ușorsălegestionezelazonadeopsșideaiciconfuzia.
## Aplicațiicumicroservicii
Reamintimdelacursgenezatermenuluidemicroserviciu.Termenuldemicroaplicațiia
fostintrodusîn2011decătreJamesLewisdelaThoughtWorkscareaînceputsăstudiezemicro
aplicațiile.Acestaeraunmodeldeproiectarecareîncepusedejasăfieutilizatdeunelecompanii
careutilizauSOC.Ulterior,în2012,înurmaunordiscuțiilauncongresdespecialitate,s-a
modificatînmicroserviciipentruaevitaconfuziile.
## Încazulaplicațiilorwebbazatepemicroserviciiseutilizeazăoabordaresimplă,
discutabilă(veziproblemeleAPI)undedacăsuntcorectproiectatesepoateajungecasăfie
formatedintr-osuitădeserviciimici,executateindependent,carecomunicăprinmecanisme
simple(lightweight)bazatepeHTTP.
## Pentrumaimultedetalii,vezi:
BuildingMicroservices(2015)-SamNewman
Hands-onmicroserviceswithKotlin(2018)-JuanAntonioMedinaIglesias
## Principiigeneralepentruproiectareacumicroservicii
## Înprimulrândvăreamintesccăpentruceicarevorsăfacăproiectarecorectătrebuiesă
porneascădelaprincipiilediscutatelacursacărorgraddegeneralitateestemaimareșiastfel
acoperămajoritateaproblemelorcarepotapăreaînproiectare.Pentrusimpliimplementatori
multefirmedisperateșinunumaiauîncercatsăcreezeunfelderezumatminimalastfelîncât
implementatorulsăpriceapăvagproiectulvenitdinzonaierarhicădeproiectare.Tocmaipentrua
puteafacediferențaîntreacestedouăabordărivomprezentaacestesimplificăridarnuuitațică
elereprezintăabilitățiincorecteșiautolimitativepentruunproiectantdearhitecturiserioase
(dimensiunimediișimari)cumicroservicii.Vărogsănuconfundațiacestrezumatpentru
indienicuprincipiileSOLIDpentrumicroserviciicaresuntdiferitedeprincipiileșimodelelede
proiectareprimare.Săîncepemcurezumatul:
Modelareținândcontdegraful(urile)parțialsautotalasociatafacerii.Dupăcumam
discutatlacursdelaSOAacestaafostunprimdezideratalcomunitățiideafaceri-potrivirea
proiectăriiculumeareală.Totușideabiaabordareabazatăpemicroserviciiareflexibilitatea
necesarăîndepliniriiunuiastfeldedeziderat.Esteclarcășipentruaplicațiilemicitrebuie
măcaroechipădacănumăcarunsoftwarearchitectcuexperiențădeminimcinciani.
Responsabilitateunică(singleresponsibility)-Fiecaremicroserviciuareca
responsabilitateosingurăpartedinfuncționalitateaaplicației,iaracearesponsabilitateeste
încapsulatăîninteriorullui.Nuuitațicataloageledeserviciișicompunereadeservicii.

## Introducere
## 2
## Aceastăsimplificareconducedemulteoriînfazadereproiectarepentrunorlaspargerea”cu
ciocanul”înniștemonolițimaimiciaiaplicațieișiimplementarealorcamicroservicii.
AcestecomportamentesuntdesîntâlniteînproiectareaSOA(carearemulteexemple)de
undeșiconfuziamajorăpesubiect.Decinuscăpațidecursșidecititînpluspelângăel.
Ascundereaimplementării-Microserviciileauuncontract(ointerfață)care
reprezintăexactfuncționalitateaprimarăsaumaicomplexădeafaceripecareo
implementează.Dinconfuziacuproiectareaorientatăpeobiectcombinatăcuneînțelegerea
principiilorpentrumicroserviciiînacestcazfiesefaceospargerepreamare(nuseurmărește
funcționalitateadeafacerișireutilizare)fiedămiarpestemicromonoliți(veziobservațiade
lacazulanterior).Detaliileinternenuartrebuidarpotfilarândullorimplementatedealte
serviciideafacerisauajutătoare.
Izolare(Isolation)-Unmicroserviciuprinînsățidefinițialuiesteizolatderestul
entitățilordinnordatorităexecuțieivirtualizate.Nudiscutămaiciproblemeledesecuritate
alemașinilorvirtualesaumanieraîncaretrebuieproiectatăsiguroaplicațiebazatăpeMSA.
## Încazulacesteivirtualizărimașinavirtualăcareîlîncapsuleazăpoateconțineînafara
suportuluinecesarbuneiluiexecuțiichiarșioformăavansatădevirtualizareinclusivun
SGBDDACĂacestlucruarezultatdinfazadeanalizășiproiectare.
Instalareindependentă(Independentlydeployable)-estecaracteristicăemergentă
dinfaptulcăcodulșicemaitrebuieesteîncapsulatîntr-omașinăvirtuală.
Creatpentrugestiuneaposibilelorerori(Buildforfailure)-Dupăcumamdiscutat
lacursaceastăsimplificaresereferăsimultanlamaimulteaspectespecificerezilienței.
Reamintimpescurtcăacesteaspectesuntluatediferitînconsiderareînfuncțiedenivelulde
analizăarhitecturalăpecareneaflăm.Lanivelulglobalprinacestprincipiuseînțelege
abilitateaderestaurareautomatăfărăpierderededateazoneiafectatedindiversemotive(de
lasimpleeroricarevindintr-oanalizăincompletăînfazaprimarădeproiectare,trecândprin
implementaredefectuoasășiajungândlaatacuricibernetice).Pemăsurăcegraduldedetalii
creșteseobservăcăpentruaobținecomportamentulglobalașteptatavemnevoieatâtde
abordărimoderneînproiectareaMSA(vezistratuldeinstrumentațieidiscutatlacurs)
combinatecuabordărileclasicepecarele-ațiîntâlnitșilaminedarșilaaltematerii.Ne
referimlaproiectareacutratareaerorilor,mecanismedetratareaerorilorinclusivcele
avansatecarepermitrestaurareaautomată.Deoarecedinnefericireînmajoritateacazurilorîn
cazuldezvoltăriideMSAselucreazăsubunAgileprostînțelesșiimplementatvomprezenta
maijososimplificarerezultatădinacesteeroriconceptualepecareovețigăsidesreferităși
utilizată(oseriedepunctecheieminimalecaretrebuieurmăritedardinnefericireelenuțin
contdecâtdespecificulCI/CDdinDevOpsneglijânddupăcumamspusanalizași
proiectareastrictnecesare):
Upstream=înțelegereafeluluiîncaredezvoltatoriiosătrimităsaunu
notificărideeroareclienților,ținândtotodatăcontdeevitareacuplării
Downstream=cumvorgestionadezvoltatoriidefectareaunuimicroserviciu
sauaunuisubsistemalacestuia(deex.unsistemdegestiuneabazelordedate).
Logging=scriereatuturorerorilorîntr-unjurnaldeexecuțiecarepoatefilocal
sauglobal,ținândcontdecâtdedesserealizeazăsalvareaacestorinformații,de
cantitateadedateșicumpotfiacesteaaccesate.Deasemenea,trebuieluateîn
considerareșicazurispeciale,cumarfiinformațiisensibileșiimplicațiide
performanță.
Monitoring=Monitorizareatrebuiesăfieproiectatăcumareatenție.Este
foartedificildegestionatoeroarefărăinformațiilepotriviteînsistemelede

## Laborator6
## 3
monitorizare.Dezvoltatoriitrebuiesădeterminecareelementealeaplicațieiau
informațiisemnificative.
Alerting=sereferălaabordareavecheautilizăriidedeclanșatoare(defapt
generaredeevenimente)cuprivirelaceseîntâmplăînsistem.Reamintimcălaora
actualăseconstatăcăserenunțălaacesteabordărișisetreceînMSAînutilizarea
arhitecturilorflux(vezicursul)chiardacăevenimentelerămâninimamodelului
declanșareavafiintrinsecădominant(conformautomatuldestareasociat)șinu
inverscapânăacum.
Recovery=Proiectareatrebuiesăconținășimetodelespecificereveniriiîn
cazuluneierori(veziobsdemaisus).Revenireaautomată(automaticrecovery)este
ideală,daravândînvederecăaceastapoateeșua,nutrebuieevitatărevenirea
manuală(manualrecovery).Înnoileabordăridefluxaceastaîncepesăscadăca
importanță-veziteoriaautomatelorfinite.Însăvețiîntâlniabordărivechidin
DevOpsșicuinfluențemasiveînproiectareaconfuzieidintreSOAșiMSAcaremai
aunevoiedeutilizareapescarălargăaacestorcârpăceli.Încazuluneiproiectăridela
zeroașacevasepoateobține(cudestulăgreutate)doardacăsemergepeabordarea
completămultiniveldeechipedearhitecțisoftwareprezentatălacurs(undevis-a
spussăcitițiînpluspesubiect-măcarTOGAFcomplet)
Fallbacks=Unmecanismbundetratareaerorilorpermitecaaplicațiasă
funcționezeîncontinuaredupăaparițiauneieroriînsistem,întimpdedezvoltatorii
lucreazăsărezolveproblemarespectivă.
Scalabilitate/Multiplicitate/Multiplicabilitate(Scalability)-Microserviciiletrebuie
săpoatăfireplicateindependent.Acestlucruținedemanieradeproiectareutilizatășiaici
modeleledeproiectarespecificesuntcriticedeoareceprinutilizarealorseobservăimediatîn
proiectdacăîncapsulareaîntr-omașinăvirtualăafostfăcutăținândcontdeacesteprincipii
saunu(dacănuamocuplaredeexîntreserviciușiuneledindependențelelui(careînmod
normalartrebuisăfietotmicroserviciideafacerisauajutătoare).
Automatizarea(Automation)-Microserviciiletrebuieproiectateținândcontde
lanțulspecificCI/CD,delaconstruireșitestarepânălainstalareșimonitorizare.Modelul
pentrudezvoltare/integrarecontinuăCI/CDtrebuieproiectatdelaînceputularhitecturii.
Domain-DrivenDesign(DDD)
## Proiectareabazatăpeanalizadomeniuluireprezintăomanierăpentrudezvoltarea
aplicațiilorcomplexeprinconectareacontinuăaimplementăriilaunmodel(careevoluează
continuu)aconceptelorbusinessdebază.Defapteareprezintăiarosimplificaremasivăa
abordărilorspecificesistemelormari(TOGAFetc).Evidentcăcineaînțelesproiectareamare
poateurmaacesteregulisimplecinenulevainterpretacompletaiureacurezultatecaredejase
vădmaialesînconjuncțiecuunAgileprostimplementat.Lacursv-amexplicatcumpebaza
analizeiworkflow-urilorcomplexealeuneiorganizațiiînprimulrândsestabileștedacănuexistă
dejaoontologiecomplexăspecificedomeniului(lor)deafacerigestionatedeaceasta.Apoidacă
nus-arealizatreorganizareagrafurilorcomplexedeworkflowsetrecelafazadereorganizarea
lorținândcontdeunnumărvariabildedimensiuniînfuncțiedespecificulșiconstrângerileunei
afaceri.Înmomentulîncares-aîncercatsimplificareacestuiprocescomplex(probabildecătre
dezvoltatorifărăexperiențăînproiectaredesistememari)eiauluat-oinvers(celmaiprobabil
datorităinfluențeidegândiredelaprogramareaorientatăobiectșiceamodularădarșiconfuziei
gravecareconducelaideeafixăcăprincipiileOOPpotfidirectutilizateînprogramareaMSA
ceeaceestecompletgreșit!DecivărogînțelegețișiînvățațisăaplicațicorectMĂCARSOLID
pentrumicroservicii.

## Introducere
## 4
PremiseleDDD:
## •
accentulprincipalalproiectuluicadepedomeniuldebază(coredomain)șipelogica
domeniului(domainlogic)-defaptaicisefacereorganizareaînmaimultegrafuriaflateîn
planuriparaleleșiinteroperanteaworkflow-uluiafacerii)
## •
## Proiectelemaicomplexetrebuiebazatepeunmodel(oriceproiectarenevoiedeun
model(poatefide-agatasautrebuiesărezultedinanalizășiproiectare)!)
## •inițiereauneicolaborăricreativeîntreexperțiitehnicișiexperțiidindomeniupentrua
ajungedinceîncemaiaproapedemodelulconceptualalproblemei
## Problema:cândcomplexitateascapădesubcontrol,software-ulnumaipoatefiînțeles
suficientdebinepentruaputeafischimbatsauextinscuușurință.Dacăcomplexitatea
domeniuluinuestetratatăînproiectare,nuconteazăcătehnologiainfrastructuriisuportestebine
concepută.Exactcenuauînțelesdezvoltatoriidinproiectareamare!
## Principiiledeproiectare:
•Contextmărginit(Boundedcontext):Cândseabordeazăunsistemcomplex,de
obiceiseabstractizeazăîntr-unmodelcaredescrieaspectelediferitealesistemuluișicum
poatefifolositpentruarezolvaprobleme.Cândexistămaimultemodele,iarcoduldebază
aldiferitelormodeleestecombinat,software-uldevineplindeerori(buggy),nesigurșigreu
deînțeles.Seobservăiarcănupomeneștenimenideanalizășiproiectaredarmaimuțacare
scriecodaînceputsăsimtăcaaraveanevoiedeașaceva.ÎnDDD,sedefineștecontextulîn
careseaplicăunmodel,sestabilescexplicitgranițeleînceeacepriveșteorganizareaechipei
șiutilizareaînanumitepărțialeaplicației,păstrândmodelulconsecventcuacestelimite-
veziobservațiacugrafurile.
•Limbajgenericspecific(Ubiquitouslanguage):ÎnDDDtrebuiealcătuitunlimbaj
comunșirigurosîntredezvoltatorișiutilizatori.Acestlimbajtrebuiesăfiebazatpemodelul
dedomeniu,ajutândînaaveaoconversațiegeneralăîntretoțiexperțiidindomeniu,acest
lucrufiindesențiallaabordareatestării(dehdacănuaplicămprincipiilecorectedeanaliză
primarănuobținemontologiașiatuncioluămdupăurecheșicaatarearezultatacestașazis
limbaj).
## •
Capturarea/mapareacontextului(Contextmapping):Într-oaplicațiededimensiuni
mari,proiectatăpentrumaimultecontextemărginite(boundedcontexts),sepoatepierde
vedereadeansamblu.Inevitabil,contextelemărginitevorfinevoitesăcomunicedateîntre
ele.Omaparedecontextesteovederedeansamblu(globalview)asuprasistemuluicaun
întreg,careilustreazămanieraîncarecontextelemărginiteartrebuisăcomuniceîntreele.
## (dupăcumspuneamoiaudesusînjos(bottomup)șilasfârșitsecrucesccănuseamănăcu
ceaveanevoieclientul).Poateașa(cuacesteanalizecomparative)amsăvăconvingde
importanțarespectăriifazelordeanalizășiproiectareînmanieraîncarevăzicîncepânddin
anulII.

## Laborator6
## 5
## Exempludemaparedecontext
Pentrumaimultedetalii,vezicartea„Domain-DrivenDesign”scrisădeEricEvans,
precumșicomunitateaDDD:https://dddcommunity.org/.
FolosireaDDDînmicroservicii
Aicivomîncercasăsimplificăm(veziobservațiileanterioaredespreimplementator)anumite
aspectecomplexecarețindeproiectareacorectăasistemelormaricumicroservicii.Evidentse
pierdeatâtdemultcănusemaiînțelegemainimic.
•BoundedContext-Nutrebuiecreatunmicroserviciucareincludemaimultdeun
contextmărginit(aiciaparerorilecelemaimaripentrucăfiecareiaunsubfluxdeafaceriși
apoiîncearcăsălepotrivească“cuciocanu”-problemaabordăriibottomup)
## •
UbiquitousLanguage-Dezvoltatoriitrebuiesăseasigurecămanieradecomunicare
utilizatăestesuficientdegeneralvalabil,astfelîncâtoperațiileșiinterfețelecaresuntexpuse
săfieexprimateutilizândlimbajuldomeniuluicontext(veziobservațiileanterioare)
## •
ContextModel-Modelulutilizatdemicroserviciutrebuiedefinitîntr-uncontext
mărginitșisăfoloseascăunlimbajgeneric(ubiquitouslanguage),chiarșipentruentitățicare
nusuntexpuseînniciointerfațăpecareooferămicroserviciul
•ContextMapping-Trebuieexaminatcontextulmărginitalîntreguluisistempentrua
înțelegedependențeleșicuplareamicroserviciilor.Adicăîncearcăsăsepotriveascăcuceera.
## Evidentcăfărăanalizaglobalăsescapăfoartemulteaspectedinvederebamairăuînlocsă
îiajuteîiîncurcăpetermenlung.Aceastasedatoreazărezistențeilaschimbareînorganizații
carenuînțelegnecesitateareanalizăriișireproiectăriiworkflow-urilorșiceroimplementare
asituațieiexistente.MaialesîncazulUEdeRomanianicinumaispunaceastaabordareeste
nesănătoasă).DinaceastăcauzăabordareaDDD&Agilepuresterecomandatănumaipentru
proiectelededimensiunimicisaumediispremici.EvidentcăpânășilaWebrămânevalabilă
observația.

## Exemple
## 6
## Exemple
PentruunexempludemicroserviciiînKotlincuframework-ulKtor,serecomandă
parcurgereamodeluluidelaadresa:https://dzone.com/articles/kotlin-
microservices-with-ktor
Exemplul1:BeerApp
Cerință:SăsecreezeunmicroserviciuînKotlincaresăgestionezeobazădedatecu
tipuriledebere.AplicațiaKotlinvacomunicacuointerfațădetipCLIdinpythonprin
intermediulframework-uluiRabbitMQ.BazadedateutilizatădemicroserviciuesteSQLite3.
## Diagramademicroservicii
Încontinuare,seprezintădiagramaUMLpentrumicroserviciulBeerDAOMicroservice.
PentrustructurareamicroserviciuluiBeerDAOMicroservice,s-aufolosit3nivele,fiecare
avândrolșiresponsabilitatediferităîncadrulaplicației:
## -prezentare(responsabildeinteracțiuneacuutilizatorul)
## -business(responsabildeparteadelogică,adicăprocesarea/agregareadatelor)
## -persistență(responsabilpentrupăstrareapetermenlungainformațiilorșicaredeobicei
seocupăcutransformăribidirecționaleîntrereprezentăriledatelorcaresuntspecificesistemelor
degestiuneabazelordedate(SGBD)șiceleutilizateînaplicație(deobiceiînniveluldeafaceri
șimairar(deobiceidacăaplicațianuarestratexplicitpentruafacerisauesteproiectatăcucele
dinparteadeprezentare).
## Astfel,ocereredinniveluldeprezentarevatreceprinniveluldebusinesspentruaaccesa
niveluldepersistență.Acestlucruproduceodecuplareîntremoduleleaplicațieisiprevineca
aplicațiasădevinădificildeîntreținut.
## Reamintimcăexistășimodelulcudouăniveluricareestespecificunoraplicațiifoarte
simpleundeniveluldebusinessșiniveluldepersistențăsuntcombinateîntr-unsingurnivel(de
ex.atuncicândcomponenteledepeniveluldepersistențăpotfiintegratedirectîncomponente
depeniveluldebusines).Totușiînaplicațiiserioasesemergepeabordareamultilayer-multitier.
Reamintimcăaceastăabordareesteogenerațizareaaabordăriicutreinivelurideoarecefiecare
nivelesteseparatînsubstraturifiecareavândfiecarerolulluispecific(deex.stratdevalidarea
datelor,straturidesecuritatesauchiarstraturideafaceri).
Atenție:Nuconfundațiserviciilecacomponentealeniveluluidebusinessîncadrulunei
aplicații,cuserviciiledincadrulSOA(eng.ServiceOrientedArchitecture)deoareceîntâiseface

## Laborator6
## 7
proiectareașiapoiimplementarea(lucrunuprearespectatdinnefericire)șiatuncipotexista
suprapuneriparțialeîncazulimplementăriisaunu.
DiagramaUMLpentrumicroserviciulBeerDAOMicroservice
Exemplul1:Configurări
Similarculaboratorul5deladisciplinaSistemeDistribuite,seconfigureazăurmătoarele
cozidemesaje,shimburi(exchange)șicheiderutareîninterfațaRabbitMQ(localhost:15672):
## unexchange:beerapp.direct
## douăcozi
## beerapp.queue
## beerapp.queue1

## Exemple
## 8
## douălegături(binding):
## beerapp.queue->beerapp.direct,beerapp.routingkey
## beerapp.queue1->beerapp.direct,beerapp.routingkey1
## Structuraproiectului
IerarhiaaplicațieiBeerApp
## Configurareaparametrilordinfișierulappplication.properties
spring.datasource.url=jdbc:sqlite:beer.db
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=student
spring.rabbitmq.password=student
beerapp.rabbitmq.queue=beerapp.queue1
beerapp.rabbitmq.exchange=beerapp.direct
beerapp.rabbitmq.routingkey=beerapp.routingkey
## Seremarcăparametrulspring.datasource.urlcareprecizeazăfaptulcăjdbc-ul(java
databaseconnector)vautilizaobazădedatedetipsqlitedenumităbeer.db.
## Creareaproiectului
Proiectulsecreeazăconforminstrucțiunilordinlaboratorul5deladisciplinaSisteme
## Distribuite.
## Seadaugădependențelepentrusqliteînfișierulpom.xml:
## <dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-jdbc</artifactId>
## </dependency>
## <dependency>
<groupId>org.xerial</groupId>

## Laborator6
## 9
<artifactId>sqlite-jdbc</artifactId>
## <version>3.36.0.2</version>
## </dependency>
Exemplul1:Codulsursă
MicroserviciulBeerDAOMicroservice
BeerApp.kt
packagecom.sd.laborator
importorg.springframework.boot.autoconfigure.SpringBootApplication
importorg.springframework.boot.runApplication
@SpringBootApplication
classBeerApp
funmain(args:Array<String>){
runApplication<BeerApp>(*args)
## }
Beer.kt
packagecom.sd.laborator.models
classBeer(privatevarid:Int,privatevarname:String,privatevar
price:Float){
varbeerID:Int
get(){
returnid
## }
set(value){
id=value
## }
varbeerName:String
get(){
returnname
## }
set(value){
name=value
## }
varbeerPrice:Float
get(){
returnprice
## }
set(value){
price=value
## }
overridefuntoString():String{
return"Beer[id=$beerID,name=$beerName,price=$beerPrice]"
## }
## }
BeerRowMapper.kt

## Exemple
## 10
packagecom.sd.laborator.persistence.mappers
importcom.sd.laborator.models.Beer
importorg.springframework.jdbc.core.RowMapper
importjava.sql.ResultSet
importjava.sql.SQLException
classBeerRowMapper:RowMapper<Beer?>{
@Throws(SQLException::class)
overridefunmapRow(rs:ResultSet,rowNum:Int):Beer{
returnBeer(rs.getInt("id"),rs.getString("name"),
rs.getFloat("price"))
## }
## }
IBeerRepository.kt
packagecom.sd.laborator.persistence.interfaces
importcom.sd.laborator.models.Beer
interfaceIBeerRepository{
//Create
funcreateTable()
funadd(beer:Beer)
//Retrieve
fungetAll():MutableList<Beer?>
fungetByName(name:String):Beer?
fungetByPrice(price:Float):MutableList<Beer?>
//Update
funupdate(beer:Beer)
//Delete
fundelete(name:String)
## }
BeerRepository.kt
packagecom.sd.laborator.persistence.repositories
importcom.sd.laborator.models.Beer
importcom.sd.laborator.persistence.interfaces.IBeerRepository
importcom.sd.laborator.persistence.mappers.BeerRowMapper
importorg.springframework.beans.factory.annotation.Autowired
importorg.springframework.dao.EmptyResultDataAccessException
importorg.springframework.jdbc.UncategorizedSQLException
importorg.springframework.jdbc.core.JdbcTemplate
importorg.springframework.jdbc.core.RowMapper
importorg.springframework.stereotype.Repository
@Repository
classBeerRepository:IBeerRepository{
@Autowired
privatelateinitvar_jdbcTemplate:JdbcTemplate

## Laborator6
## 11
privatevar_rowMapper:RowMapper<Beer?>=BeerRowMapper()
overridefuncreateTable(){
_jdbcTemplate.execute("""CREATETABLEIFNOTEXISTSbeers(
idINTEGERPRIMARYKEY
## AUTOINCREMENT,
nameVARCHAR(100)UNIQUE,
priceFLOAT)""")
## }
overridefunadd(beer:Beer){
try{
_jdbcTemplate.update("INSERTINTObeers(name,price)
VALUES(?,?)",beer.beerName,beer.beerPrice)
}catch(e:UncategorizedSQLException){
println("Anerrorhasoccurredin
${this.javaClass.name}.add")
## }
## }
overridefungetAll():MutableList<Beer?>{
return_jdbcTemplate.query("SELECT*FROMbeers",_rowMapper)
## }
overridefungetByName(name:String):Beer?{
returntry{
_jdbcTemplate.queryForObject("SELECT*FROMbeersWHERE
name='$name'",_rowMapper)
}catch(e:EmptyResultDataAccessException){
null
## }
## }
overridefungetByPrice(price:Float):MutableList<Beer?>{
return_jdbcTemplate.query("SELECT*FROMbeersWHEREprice<=
$price",_rowMapper)
## }
overridefunupdate(beer:Beer){
try{
_jdbcTemplate.update("UPDATEbeersSETname=?,price=?
WHEREid=?",beer.beerName,beer.beerPrice,beer.beerID)
}catch(e:UncategorizedSQLException){
println("Anerrorhasoccurredin
${this.javaClass.name}.update")
## }
## }
overridefundelete(name:String){
try{
_jdbcTemplate.update("DELETEFROMbeersWHEREname=?",
name)
}catch(e:UncategorizedSQLException){
println("Anerrorhasoccurredin
${this.javaClass.name}.delete")
## }
## }

## Exemple
## 12
## }
IBeerService.kt
packagecom.sd.laborator.business.interfaces
importcom.sd.laborator.models.Beer
interfaceIBeerService{
funcreateBeerTable()
funaddBeer(beer:Beer)
fungetBeers():String
fungetBeerByName(name:String):String?
fungetBeerByPrice(price:Float):String
funupdateBeer(beer:Beer)
fundeleteBeer(name:String)
## }
BeerService
packagecom.sd.laborator.business.services
importcom.sd.laborator.business.interfaces.IBeerService
importcom.sd.laborator.models.Beer
importcom.sd.laborator.persistence.interfaces.IBeerRepository
importorg.springframework.beans.factory.annotation.Autowired
importorg.springframework.stereotype.Service
importjava.util.regex.Pattern
@Service
classBeerService:IBeerService{
@Autowired
privatelateinitvar_beerRepository:IBeerRepository
privatevar_pattern:Pattern=Pattern.compile("\\W")
overridefuncreateBeerTable(){
_beerRepository.createTable()
## }
overridefunaddBeer(beer:Beer){
if(_pattern.matcher(beer.beerName).find()){
println("SQLInjectionforbeername")
return
## }
_beerRepository.add(beer)
## }
overridefungetBeers():String{
valresult:MutableList<Beer?>=_beerRepository.getAll()
varstringResult:String=""
for(iteminresult){
stringResult+=item
## }
returnstringResult

## Laborator6
## 13
## }
overridefungetBeerByName(name:String):String?{
if(_pattern.matcher(name).find()){
println("SQLInjectionforbeername")
returnnull
## }
valresult=_beerRepository.getByName(name)
returnresult.toString()
## }
overridefungetBeerByPrice(price:Float):String{
valresult=_beerRepository.getByPrice(price)
varstringResult:String=""
for(iteminresult){
stringResult+=item
## }
returnstringResult
## }
overridefunupdateBeer(beer:Beer){
if(_pattern.matcher(beer.beerName).find()){
println("SQLInjectionforbeername")
return
## }
_beerRepository.update(beer)
## }
overridefundeleteBeer(name:String){
if(_pattern.matcher(name).find()){
println("SQLInjectionforbeername")
return
## }
_beerRepository.delete(name)
## }
## }
BeerDAOController.kt
packagecom.sd.laborator.presentation.controllers
importcom.sd.laborator.business.interfaces.IBeerService
importcom.sd.laborator.models.Beer
importcom.sd.laborator.presentation.config.RabbitMqComponent
importorg.springframework.amqp.core.AmqpTemplate
importorg.springframework.amqp.rabbit.annotation.RabbitListener
importorg.springframework.beans.factory.annotation.Autowired
importorg.springframework.stereotype.Component
@Component
classBeerDAOController{
@Autowired
privatelateinitvar_beerService:IBeerService
@Autowired
privatelateinitvar_rabbitMqComponent:RabbitMqComponent

## Exemple
## 14
privatelateinitvar_amqpTemplate:AmqpTemplate
@Autowired
funinitTemplate(){
this._amqpTemplate=_rabbitMqComponent.rabbitTemplate()
## }
## //citescdinqueue1
## //scriuinqueue
@RabbitListener(queues=["\${beerapp.rabbitmq.queue}"])
funreceiveMessage(msg:String){
val(operation,parameters)=msg.split('~')
varbeer:Beer?=null
varprice:Float?=null
varname:String?=null
//id=1;name=Corona;price=3.6
if("id="inparameters){
println(parameters)
valparams:List<String>=parameters.split(';')
try{
beer=Beer(
params[0].split('=')[1].toInt(),
params[1].split('=')[1],
params[2].split('=')[1].toFloat()
## )
}catch(e:Exception){
print("Errorparsingtheparameters:")
println(params)
return
## }
## }elseif("price="inparameters){
price=parameters.split('=')[1].toFloat()
## }elseif("name="inparameters){
name=parameters.split("=")[1]
## }
println("Parameters:$parameters")
println("Name:$name")
println("Price:$price")
println("Beer:$beer")
valresult:Any?=when(operation){
"createBeerTable"->_beerService.createBeerTable()
"addBeer"->_beerService.addBeer(beer!!)
"getBeers"->_beerService.getBeers()
"getBeerByName"->_beerService.getBeerByName(name!!)
"getBeerByPrice"->_beerService.getBeerByPrice(price!!)
"updateBeer"->_beerService.updateBeer(beer!!)
"deleteBeer"->_beerService.deleteBeer(name!!)
else->null
## }
println("Result:$result")
if(result!=null)sendMessage(result.toString())
## }
privatefunsendMessage(msg:String){
println("Messagetosend:$msg")

## Laborator6
## 15
this._amqpTemplate.convertAndSend(_rabbitMqComponent.getExchange(),
_rabbitMqComponent.getRoutingKey(),msg)
## }
## }
RabbitMqComponent
packagecom.sd.laborator.presentation.config
import
org.springframework.amqp.rabbit.connection.CachingConnectionFactory
importorg.springframework.amqp.rabbit.connection.ConnectionFactory
importorg.springframework.amqp.rabbit.core.RabbitTemplate
importorg.springframework.beans.factory.annotation.Value
importorg.springframework.context.annotation.Bean
importorg.springframework.stereotype.Component
@Component
classRabbitMqComponent{
@Value("\${spring.rabbitmq.host}")
privatelateinitvarhost:String
@Value("\${spring.rabbitmq.port}")
privatevalport:Int=0
@Value("\${spring.rabbitmq.username}")
privatelateinitvarusername:String
@Value("\${spring.rabbitmq.password}")
privatelateinitvarpassword:String
@Value("\${beerapp.rabbitmq.exchange}")
privatelateinitvarexchange:String
@Value("\${beerapp.rabbitmq.routingkey}")
privatelateinitvarroutingKey:String
fungetExchange():String=this.exchange
fungetRoutingKey():String=this.routingKey
@Bean
privatefunconnectionFactory():ConnectionFactory{
valconnectionFactory=CachingConnectionFactory()
connectionFactory.host=this.host
connectionFactory.username=this.username
connectionFactory.setPassword(this.password)
connectionFactory.port=this.port
returnconnectionFactory
## }
@Bean
funrabbitTemplate():RabbitTemplate=
RabbitTemplate(connectionFactory())
## }
MicroserviciulBeerCLI
importpika
fromretryimportretry

## Exemple
## 16
classRabbitMq:
config={
## 'host':'0.0.0.0',
## 'port':5678,
## 'username':'student',
## 'password':'student',
## 'exchange':'beerapp.direct',
## 'routing_key':'beerapp.routingkey1',
## 'queue':'beerapp.queue'
## }
credentials=pika.PlainCredentials(config['username'],
config['password'])
parameters=(pika.ConnectionParameters(host=config['host']),
pika.ConnectionParameters(port=config['port']),
pika.ConnectionParameters(credentials=credentials))
defon_received_message(self,blocking_channel,deliver,
properties,
message):
result=message.decode('utf-8')
blocking_channel.confirm_delivery()
try:
print(result)
exceptException:
print("wrongdataformat")
finally:
blocking_channel.stop_consuming()
@retry(pika.exceptions.AMQPConnectionError,delay=5,jitter=(1,3))
defreceive_message(self):
## #automaticallyclosetheconnection
withpika.BlockingConnection(self.parameters)asconnection:
## #automaticallyclosethechannel
withconnection.channel()aschannel:
channel.basic_consume(self.config['queue'],
self.on_received_message)
try:
channel.start_consuming()
#Don'trecoverconnectionsclosedbyserver
exceptpika.exceptions.ConnectionClosedByBroker:
print("Connectionclosedbybroker.")
#Don'trecoveronchannelerrors
exceptpika.exceptions.AMQPChannelError:
print("AMQPChannelError")
#Don'trecoverfromKeyboardInterrupt
exceptKeyboardInterrupt:
print("Applicationclosed.")
defsend_message(self,message):
## #automaticallyclosetheconnection
withpika.BlockingConnection(self.parameters)asconnection:
## #automaticallyclosethechannel
withconnection.channel()aschannel:
self.clear_queue(channel)
channel.basic_publish(exchange=self.config['exchange'],

## Laborator6
## 17
routing_key=self.config['routing_key'],
body=message)
defclear_queue(self,channel):
channel.queue_purge(self.config['queue'])
defprint_menu():
print('0-->Exitprogram')
print('1-->addBeer')
print('2-->getBeers')
print('3-->getBeerByName')
print('4-->getBeerByPrice')
print('5-->updateBeer')
print('6-->deleteBeer')
returninput("Option=")
if__name__=='__main__':
rabbit_mq=RabbitMq()
rabbit_mq.send_message("createBeerTable~")
whileTrue:
option=print_menu()
ifoption=='0':
break
elifoption=='1':
name=input("Beername:")
price=float(input("Beerprice:"))
rabbit_mq.send_message("addBeer~id=-
## 1;name={};price={}".format(name,price))
elifoption=='2':
rabbit_mq.send_message("getBeers~")
rabbit_mq.receive_message()
elifoption=='3':
name=input("Beername:")
rabbit_mq.send_message("getBeerByName~name={}".format(name))
rabbit_mq.receive_message()
elifoption=='4':
price=float(input("Beerprice:"))
rabbit_mq.send_message("getBeerByPrice~price={}".format(price))
rabbit_mq.receive_message()
elifoption=='5':
id=int(input("BeerID:"))
name=input("Beername:")
price=float(input("Beerprice:"))
rabbit_mq.send_message("updateBeer~id={};name={};price={}".format(id,
name,price))
rabbit_mq.receive_message()
elifoption=='6':
name=input("Beername:")
rabbit_mq.send_message("deleteBeer~name={}".format(name))
else:
print("Invalidoption")

## Exemple
## 18
Pentrutestareaexemplului,dinIntelliJseexecutăMaven->Lifecycle->clean+
compile+package.Lafinal,sedeschidedirectorultargetcreatșiseexecutăînterminal
comanda:
java-jarBeerApp-1.0.0.jar
PentruaporniinterfațaCLI,seexecutăîntr-unterminaldeschisîndirectorulcuaplicația
pythoncomenzile:
python3-mvenvenv
sourceenv/bin/activate
pip3installpika==1.1.0retry==0.9.2
python3beer_app_cli.py
Exemplul2:LibraryApp
Cerință:SăsescrieunprogramKotlincaresărealizezeprinintermediulunui
microserviciuWEBgestiuneauneibiblioteciutilizândprincipiileSOLID.Aplicațiavaconține
treimodurideafișareadatelor(HTML,JSONșiRaw)șivaexpuneutilizatoruluiprininterfață
funcționalitățidetipCRUD(Create,Retrieve,Update,Delete).
Spredeosebiredeexemplulanterior,microserviciulLibraryAppMicroservicevafidetip
Web,expunândcătremicroserviciuldeGUImetodeledeafișarealebibliotecii.Arhitecturile

## Laborator6
## 19
pentrumicroserviciulLibraryAppMicroservice,respectivCacheMicroservicesuntreprezentate
îndiagrameledeclasedemaijos.
## Observație:funcționalitățilemarcatecuroșudindiagramademaisusnuseregăsescîn
exemplulcurent.Acesteatrebuieimplementateîntemapeacasă.
DiagramaUMLalmicroserviciuluiLibraryAppMicroservice

## Exemple
## 20
DiagramaUMLalmicroserviciuluiCacheMicroservice
## Înacestexemplus-aproiectatînașafelpentrucaniveluldebusinessșinivelulde
persistențăsăfoloseascăDTO-uri(eng.DataTransferObject)separate(business:models,
persistență:entities),cuscopuldeaîncapsulaanumiteatributealemodeluluideCache(precum
timestamp).

## Laborator6
## 21
InterfațagraficăLibraryAppGUIrealizatăcuPyQt5
## Structuraproiectului
IerarhiaaplicațieiLibraryAppMicroservice
## Creareaproiectului

## Exemple
## 22
Proiectulsecreeazăsimilarcuexemplulanterior.Lafinal,semaiadaugădependențade
spring-boot-starter-webînfișierulpom.xml:
## <dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-web</artifactId>
## </dependency>
Exemplul2:Codulsursă
MicroserviciulLibraryAppMicroservice
LibraryApp.kt
packagecom.sd.laborator
importorg.springframework.boot.autoconfigure.SpringBootApplication
importorg.springframework.boot.runApplication
@SpringBootApplication
classLibraryApp
funmain(args:Array<String>){
runApplication<LibraryApp>(*args)
## }
Content.kt
packagecom.sd.laborator.business.models
dataclassContent(varauthor:String?,vartext:String?,varname:
String?,varpublisher:String?)
Book.kt
packagecom.sd.laborator.business.models
classBook(privatevardata:Content){
varname:String?
get(){
returndata.name
## }
set(value){
data.name=value
## }
varauthor:String?
get(){
returndata.author
## }
set(value){
data.author=value
## }
varpublisher:String?
get(){

## Laborator6
## 23
returndata.publisher
## }
set(value){
data.publisher=value
## }
varcontent:String?
get(){
returndata.text
## }
set(value){
data.text=value
## }
funhasAuthor(author:String):Boolean{
returndata.author.equals(author)
## }
funhasTitle(title:String):Boolean{
returndata.name.equals(title)
## }
funpublishedBy(publisher:String):Boolean{
returndata.publisher.equals(publisher)
## }
## }
LibraryPrinterService.kt
packagecom.sd.laborator.business.services
importcom.sd.laborator.business.interfaces.ILibraryPrinterService
importcom.sd.laborator.business.models.Book
importorg.springframework.stereotype.Service
@Service
classLibraryPrinterService:ILibraryPrinterService{
overridefunprintHTML(books:Set<Book>):String{
varcontent:String="<html><head><title>Librariamea
HTML</title></head><body>"
books.forEach{
content+=
## "<p><h3>${it.name}</h3><h4>${it.author}</h4><h5>${it.publisher}</h5>${
it.content}</p><br/>"
## }
content+="</body></html>"
returncontent
## }
overridefunprintJSON(books:Set<Book>):String{
varcontent:String="[\n"
books.forEach{
if(it!=books.last())
content+="{\"Titlu\":\"${it.name}\",
\"Autor\":\"${it.author}\",\"Editura\":\"${it.publisher}\",
\"Text\":\"${it.content}\"},\n"

## Exemple
## 24
else
content+="{\"Titlu\":\"${it.name}\",
\"Autor\":\"${it.author}\",\"Editura\":\"${it.publisher}\",
\"Text\":\"${it.content}\"}\n"
## }
content+="]\n"
returncontent
## }
overridefunprintRaw(books:Set<Book>):String{
varcontent:String=""
books.forEach{
content+=
## "${it.name}\n${it.author}\n${it.publisher}\n${it.content}\n\n"
## }
returncontent
## }
## }
LibraryDAOService.kt
packagecom.sd.laborator.business.services
importcom.sd.laborator.business.interfaces.ILibraryDAOService
importcom.sd.laborator.business.models.Book
importcom.sd.laborator.business.models.Content
importorg.springframework.stereotype.Service
@Service
classLibraryDAOService:ILibraryDAOService{
privatevar_books:MutableSet<Book>=mutableSetOf(
## Book(
## Content(
"RobertoIerusalimschy",
"Preface.WhenWaldemar,Luiz,andIstartedthe
developmentofLua,backin1993,wecouldhardlyimaginethatit
wouldspreadasitdid....",
"ProgramminginLUA",
"Teora"
## )
## ),
## Book(
## Content(
"JulesVerne",
"Nemaipomenitisuntfranceziiastia!-Vorbiti,
domnule,vaascult!....",
"SteauaSudului",
"Corint"
## )
## ),
## Book(
## Content(
"JulesVerne",
"CuvantInainte.Imaginatiacopiilor-ziceaunmare
poetromanticspaniol-esteasemeneaunuicalnazdravan,iar
curiozitatealorepintenulce-lfugaresteprinlumeacelormai
indrazneteproiecte.",

## Laborator6
## 25
"Ocalatoriesprecentrulpamantului",
"Polirom"
## )
## ),
## Book(
## Content(
"JulesVerne",
"Parteaintai.Naufragiatiivazduhului.Capitolul1.
## Uraganuldin1865....",
"InsulaMisterioasa",
"Teora"
## )
## ),
## Book(
## Content(
"JulesVerne",
"CapitolulI.S-apusunpremiupecapulunuiom.Se
oferapremiude2000delire...",
"Casacuaburi",
"Albatros"
## )
## )
## )
overridefungetBooks():Set<Book>{
returnthis._books
## }
overridefunaddBook(book:Book){
this._books.add(book)
## }
overridefunfindAllByAuthor(author:String):Set<Book>{
return(this._books.filter{it.hasAuthor(author)}).toSet()
## }
overridefunfindAllByTitle(title:String):Set<Book>{
return(this._books.filter{it.hasTitle(title)}).toSet()
## }
overridefunfindAllByPublisher(publisher:String):Set<Book>{
return(this._books.filter
{it.publishedBy(publisher)}).toSet()
## }
## }
IHTMLPrinter
packagecom.sd.laborator.business.interfaces
importcom.sd.laborator.business.models.Book
interfaceIHTMLPrinter{
funprintHTML(books:Set<Book>):String
## }
IJSONPrinter

## Exemple
## 26
packagecom.sd.laborator.business.interfaces
importcom.sd.laborator.business.models.Book
interfaceIJSONPrinter{
funprintJSON(books:Set<Book>):String
## }
ILibraryDAOService
packagecom.sd.laborator.business.interfaces
importcom.sd.laborator.business.models.Book
interfaceILibraryDAOService{
fungetBooks():Set<Book>
funaddBook(book:Book)
funfindAllByAuthor(author:String):Set<Book>
funfindAllByTitle(title:String):Set<Book>
funfindAllByPublisher(publisher:String):Set<Book>
## }
ILibraryPrinterService
packagecom.sd.laborator.business.interfaces
interfaceILibraryPrinterService:IHTMLPrinter,IJSONPrinter,
IRawPrinter
IRawPrinter
packagecom.sd.laborator.business.interfaces
importcom.sd.laborator.business.models.Book
interfaceIRawPrinter{
funprintRaw(books:Set<Book>):String
## }
LibraryPrinterController
SeobservăfaptulcămicroserviciulareaccesprinintermediulunuiILibraryDAOService
labazadedate(pemoment,ladatelehardcodate),lipsindniveluldepersistențășitotodatăla
funcționalitățiledeprintarealeLibraryPrinter.FuncționalitățilemicroserviciuluiWEBsunt
expuselaurmătoareleURL-uri:
## »http://localhost:8080/print?format=html
## »
http://localhost:8080/find?author=Jules%20Verne
## »
http://localhost:8080/find?title=Steaua%20Sudului
»http://localhost:8080/find?publisher=Corint
packagecom.sd.laborator.presentation.controllers
importcom.sd.laborator.business.interfaces.ILibraryDAOService
importcom.sd.laborator.business.interfaces.ILibraryPrinterService
importorg.springframework.beans.factory.annotation.Autowired
importorg.springframework.stereotype.Controller
importorg.springframework.web.bind.annotation.RequestMapping

## Laborator6
## 27
importorg.springframework.web.bind.annotation.RequestMethod
importorg.springframework.web.bind.annotation.RequestParam
importorg.springframework.web.bind.annotation.ResponseBody
@Controller
classLibraryPrinterController{
@Autowired
privatelateinitvar_libraryDAOService:ILibraryDAOService
@Autowired
privatelateinitvar_libraryPrinterService:
ILibraryPrinterService
@RequestMapping("/print",method=[RequestMethod.GET])
@ResponseBody
funcustomPrint(@RequestParam(required=true,name="format",
defaultValue="")format:String):String{
returnwhen(format){
## "html"->
_libraryPrinterService.printHTML(_libraryDAOService.getBooks())
## "json"->
_libraryPrinterService.printJSON(_libraryDAOService.getBooks())
## "raw"->
_libraryPrinterService.printRaw(_libraryDAOService.getBooks())
else->"Notimplemented"
## }
## }
@RequestMapping("/find",method=[RequestMethod.GET])
@ResponseBody
funcustomFind(
@RequestParam(required=false,name="author",defaultValue
="")author:String,
@RequestParam(required=false,name="title",defaultValue=
"")title:String,
@RequestParam(required=false,name="publisher",
defaultValue="")publisher:String
):String{
if(author!="")
return
this._libraryPrinterService.printJSON(this._libraryDAOService.findAllB
yAuthor(author))
if(title!="")
return
this._libraryPrinterService.printJSON(this._libraryDAOService.findAllB
yTitle(title))
if(publisher!="")
return
this._libraryPrinterService.printJSON(this._libraryDAOService.findAllB
yPublisher(publisher))
return"Notavalidfield"
## }
## }

## Exemple
## 28
MicroserviciulLibraryAppGUI
## Încoduldemaijos,seremarcăcă,spredeosebiredeaplicațiadinlaboratorulprecedent
încarecomunicațiaerarealizatăprincozidemesaje,exemplulcurentutilizeazămodulul
requeststrimițândcereriHTTPdetipGETcătremicroserviciulWebdinKotlin.
importos
importsys
importrequests
importqdarkstyle
fromrequests.exceptionsimportHTTPError
fromPyQt5.QtWidgetsimport(QWidget,QApplication,QFileDialog,
QMessageBox)
fromPyQt5importQtCore
fromPyQt5.uicimportloadUi
defdebug_trace(ui=None):
frompdbimportset_trace
QtCore.pyqtRemoveInputHook()
set_trace()
#QtCore.pyqtRestoreInputHook()
classLibraryApp(QWidget):
ROOT_DIR=os.path.dirname(os.path.abspath(__file__))
def__init__(self):
super(LibraryApp,self).__init__()
ui_path=os.path.join(self.ROOT_DIR,'library_manager.ui')
loadUi(ui_path,self)
self.search_btn.clicked.connect(self.search)
self.save_as_file_btn.clicked.connect(self.save_as_file)
defsearch(self):
search_string=self.search_bar.text()
url=None
ifnotsearch_string:
ifself.json_rb.isChecked():
url='/print?format=json'
elifself.html_rb.isChecked():
url='/print?format=html'
else:
url='/print?format=raw'
else:
ifself.author_rb.isChecked():
url='/find?author={}'.format(
search_string.replace('','%20'))
elifself.title_rb.isChecked():
url='/find?title={}'.format(
search_string.replace('','%20'))
else:
url='/find?publisher={}'.format(
search_string.replace('','%20'))
full_url="http://localhost:8080"+url
try:
response=requests.get(full_url)
self.result.setText(response.content.decode('utf-8'))
exceptHTTPErrorashttp_err:

## Laborator6
## 29
print('HTTPerroroccurred:{}'.format(http_err))
exceptExceptionaserr:
print('Othererroroccurred:{}'.format(err))
defsave_as_file(self):
options=QFileDialog.Options()
options|=QFileDialog.DontUseNativeDialog
file_path=str(
QFileDialog.getSaveFileName(self,
'Salvarefisier',
options=options))
iffile_path:
file_path=file_path.split("'")[1]
ifnotfile_path.endswith('.json')andnot
file_path.endswith(
## '.html')andnotfile_path.endswith('.txt'):
ifself.json_rb.isChecked():
file_path+='.json'
elifself.html_rb.isChecked():
file_path+='.html'
else:
file_path+='.txt'
try:
withopen(file_path,'w')asfp:
iffile_path.endswith(".html"):
fp.write(self.result.toHtml())
else:
fp.write(self.result.toPlainText())
exceptExceptionase:
print(e)
QMessageBox.warning(self,'LibraryManager',
'Nus-apututsalvafisierul')
if__name__=='__main__':
app=QApplication(sys.argv)
stylesheet=qdarkstyle.load_stylesheet_pyqt5()
app.setStyleSheet(stylesheet)
window=LibraryApp()
window.show()
sys.exit(app.exec_())
Pentrutestareaexemplului,dinIntelliJseexecutăMaven->Lifecycle->clean+
compile,apoiMaven->Plugins->spring-boot->spring-boot:run.
## Pentruaporniinterfațagrafică,seexecutăîntr-unterminaldeschisîndirectorulcu
aplicațiapythoncomenzile:
python3-mvenvenv
sourceenv/bin/activate
pip3install-rrequirements.txt
python3library_manager.py

## Aplicaţiişiteme
## 30
## Aplicaţiişiteme
## Aplicațiidelaborator:
SăsereimplementezepersistareadatelorînexemplulLibraryApputilizândobazăde
dateSQLite(caînexemplul1),avândtabelaBookîndiagramaE-Rdemaijos.Cu
aceastăocaziesevaadăugaunniveldepersistențăcaîncazulmicroserviciul
BeerAppMicroservice.
DiagramaEntitate-Relație
Săsecombinefuncționalitateadecăutarecuceadeafișareîntr-unanumitformat
(HTML,JSON,raw)subformaunuinouconector(end-point)accesibilprintr-ocerere
(request)HTTPdetipGETcătreunURL:
http://localhost:8080/find-and-print?author=<author-name>&format=json
## Temepeacasă:
Săseimplementezeunmecanismdecachingcaresalveazăînbazadedateinterogarea
utilizatorului(dacăaceastanuexistădejaînbazadedate),rezultatulcăutăriipebaza
interogăriișidataapariției(timestamp)căutării.Sevafacediagramadeservicii(vezi
exemplelacurs)șiapoiceadeclase.VezidiagrameleUMLdeclase,precumșidiagramaE-
Rdemaisuspentrumaimultedetaliidespreimplementare.Microserviciul
CachingMicroservicetrebuiesăcomuniceprinintermediulatreicozidemesajeunapentru
trimisfisierpentruimprimanta,unapentrutrimiscomenzilepentruimprimantasiuna
pentrureceptiastariiimprimantei.SepoatemodificaLibraryPrinterMicroservicesicrea
oricateserviciisemaiconsideranecesardarcujustificare.Astfel,lafiecareinterogare
introdusădeutilizator,severificăîntâicache-ul.ÎncazulunuiHIT(interogareaamaifost
introdusăanterior),dacătimestamp-ulnuestemaivechideooră,seiarezultatuldincache.
Dacătimestamp-uldepășeșteintervaluldeoorăsauîncazulunuiMISS,serealizează
căutareapropriu-zisă,iarrezultatulesteactualizat/scrisîncache.Modalitateade
interacțiune(chain,orchestrationsaugrid)dintremicroserviciiselasălaalegereastudentului.
Observație:Pentruarespectaprincipiiledeproiectarealemicroserviciilor,trebuiecreat
unproiectnoupentrumicroserviciuldeCacheMicroservicesioricealtemaisunt
consideratenecesare.
Porninddelaexemplulrezolvatanterior,pebazaanalizeidatelordinzonadecachesăse
implementezeunmecanism(defaptunaltmicroserviciu)careprimeșteozonădedateca
fișiersaucaobiectcreeazăunarboremerkleșipoatefiutilizatpentrucăutărirapideîn
respectivazonă.Acestnoumicroserviciuvafiutilizatpentrucăutareaîncache.