# blockchain-action

## actions

- Eth_AnalyzeBalance
- Eth_CreateAddress
- Eth_Obfuscate
- --name=Eth_CollectMain --params=0 (0 - eth, 1 - contract)
- --name=Eth_Transfer --params="[from, to, amount, isContract]" (0 - eth, 1 - contract)

## run

```bash
mvn spring-boot:run -Drun.arguments="--threadPoolSize=8 --name=Eth_CollectMain --params=0"
```
