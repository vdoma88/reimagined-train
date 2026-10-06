// Uses the same registered coordinates as CartoonAvatar. Requires sharp (npm install sharp).
const fs = require('fs'), path = require('path');
const sharp = require(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES ? process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES + '/sharp' : 'sharp');
const root = path.resolve(__dirname, '..');
const specs = JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/cartoon_layers.json')));
async function render(look) {
 const keys = ['body',`bottom.${look.bottom||0}`, ...(look.boots===false?[]:[look.shoeStyle===1?'sneakers':'boots']),`top.${look.top||0}`,'face',`eyes.${look.eyes||0}`,`mouth.${look.mouth||0}`,`hair.${look.hair||0}`, ...(look.accessory===1||look.accessory===3||look.accessory===5?['glasses']:[]), ...(look.accessory===2||look.accessory===3?['cap']:[]), ...(look.accessory===4||look.accessory===5?['beanie']:[])];
 const images=[];
 for(const key of keys){let s={...specs[key]};
  if(key==='face'&&look.face===1)Object.assign(s,{y:49,height:222});
  if(key==='face'&&look.face===2)Object.assign(s,{x:63,y:43,width:274,height:234});
  let im=sharp(path.join(root,'app/src/main/assets/cartoon',s.file));
  if(s.sourceY){const m=await im.metadata();im=im.extract({left:0,top:s.sourceY,width:m.width,height:m.height-s.sourceY});}
  let input=await im.resize(Math.round(s.width),Math.round(s.height),{fit:'fill'}).toBuffer();
  if(key==='body')input=await sharp(input).composite([{input:Buffer.from('<svg width="149" height="66"><rect width="149" height="66" fill="black"/></svg>'),left:130-s.x,top:403-s.y,blend:'dest-out'}]).png().toBuffer();
  images.push({input,left:Math.round(s.x),top:Math.round(s.y)});
 }
 return sharp({create:{width:400,height:640,channels:4,background:{r:0,g:0,b:0,alpha:0}}}).composite(images).png().toBuffer();
}
(async()=>{
 const images=[];
 for(let i=0;i<8;i++)images.push({input:await render({hair:i,top:i,bottom:i%5,eyes:i%2,mouth:0,accessory:0,shoeStyle:i>=4?1:0}),left:(i%4)*400,top:Math.floor(i/4)*640});
 const target=process.argv[2]||path.join(root,'docs/cartoon-characters.png');
 const sheet=await sharp({create:{width:1600,height:1280,channels:4,background:'#f4f0eb'}}).composite(images).png().toBuffer();
 await sharp(sheet).resize(1200,960).png({palette:true,quality:85}).toFile(target);
 console.log(target);
})();
