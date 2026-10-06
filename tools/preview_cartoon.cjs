// Mirrors CartoonAvatar's registered coordinates and common body transform.
const fs = require('fs'), path = require('path');
const sharp = require(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES ? process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES + '/sharp' : 'sharp');
const root = path.resolve(__dirname, '..');
const specs = JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/cartoon_layers.json')));
async function render(look) {
 const family=look.gender==='male'?'male':'female';
 const sx=(family==='male'?1.12:.96)*(look.build===1?1.06:1), sy=(family==='male'?1.07:1)*(look.build===2?(family==='male'?1.04:1.07):1);
 const keys = ['body',`bottom.${look.bottom||0}`, ...(look.boots===false?[]:[look.shoeStyle===1?'sneakers':'boots']),`top.${look.top||0}`,`face.${family}.${look.face||0}`,`eyes.${family}.${look.eyes||0}`,`mouth.${look.mouth||0}`,`hair.${look.hair||0}`, ...(look.accessory===1||look.accessory===3||look.accessory===5?['glasses']:[]), ...(look.accessory===2||look.accessory===3?['cap']:[]), ...(look.accessory===4||look.accessory===5?['beanie']:[])];
 const images=[];
 for(const key of keys){let s={...specs[key]};
  let im=sharp(path.join(root,'app/src/main/assets/cartoon',s.file));
  if(s.sourceY){const m=await im.metadata();im=im.extract({left:0,top:s.sourceY,width:m.width,height:m.height-s.sourceY});}
  let input=await im.resize(Math.round(s.width),Math.round(s.height),{fit:'fill'}).toBuffer();
  if(key==='body')input=await sharp(input).composite([{input:Buffer.from('<svg width="149" height="66"><rect width="149" height="66" fill="black"/></svg>'),left:130-s.x,top:403-s.y,blend:'dest-out'}]).png().toBuffer();
  if(key==='body'||key.startsWith('top.')||key.startsWith('bottom.')||key==='boots'||key==='sneakers'){
   input=await sharp(input).resize(Math.round(s.width*sx),Math.round(s.height*sy),{fit:'fill'}).toBuffer();
   s.x=200+(s.x-200)*sx; s.y=267+(s.y-267)*sy;
  } else if(family==='male') {
   input=await sharp(input).resize(Math.round(s.width*.90),Math.round(s.height*.92),{fit:'fill'}).toBuffer();
   s.x=200+(s.x-200)*.90; s.y=267+(s.y-267)*.92;
  }
  images.push({input,left:Math.round(s.x),top:Math.round(s.y)});
 }
 return sharp({create:{width:400,height:640,channels:4,background:{r:0,g:0,b:0,alpha:0}}}).composite(images).png().toBuffer();
}
(async()=>{
 const images=[];
 for(let row=0;row<4;row++)for(let col=0;col<4;col++){
  const i=row*4+col, male=row>=2,j=i%8;
  images.push({input:await render({gender:male?'male':'female',hair:(male?[0,2,4,7]:[1,3,5,6])[j%4],face:j%3,eyes:Math.floor(j/3)%2,top:j,bottom:male?[0,1,3][j%3]:j%5,mouth:0,accessory:j%6,shoeStyle:j%2,build:j%3}),left:col*400,top:row*640});
 }
 const target=process.argv[2]||path.join(root,'docs/distinct-characters.png');
 const sheet=await sharp({create:{width:1600,height:2560,channels:4,background:'#f4f0eb'}}).composite(images).png().toBuffer();
 await sharp(sheet).resize(1200,1920).png().toFile(target);
 console.log(target);
})();
