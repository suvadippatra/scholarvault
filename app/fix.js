const fs = require('fs');
let file = 'app/src/main/java/com/example/ui/TransactionsScreen.kt';
let text = fs.readFileSync(file, 'utf8');

// The prefix addition caused this:
// OutlinedTextField(shape = RoundedCornerShape(24.dp), 
// it might have spaces or newlines.
text = text.replace(/OutlinedTextField\(shape = RoundedCornerShape\(24\.dp\), /g, 'OutlinedTextField(');

fs.writeFileSync(file, text);
console.log('Fixed duplicate shapes.');
