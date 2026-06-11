const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/example/ui/TransactionsScreen.kt', 'utf8');
content = content.replace(/OutlinedTextField\(/g, "OutlinedTextField(shape = RoundedCornerShape(24.dp), ");
fs.writeFileSync('app/src/main/java/com/example/ui/TransactionsScreen.kt', content);
console.log('done');
